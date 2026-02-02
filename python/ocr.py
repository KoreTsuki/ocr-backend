import json
import os
import requests
import numpy as np
import multiprocessing
from concurrent.futures import ProcessPoolExecutor
from paddleocr import PaddleOCR
from flask import Flask, request, jsonify
from pdf2image import convert_from_path, convert_from_bytes

app = Flask(__name__)

# ==========================================
# 全局变量与 Worker 设置
# ==========================================

# 注意：在多进程模式下，主进程的 ocr 对象无法直接传递给子进程。
# 我们需要定义一个全局变量，在每个子进程初始化时加载模型。
_worker_ocr = None

def init_worker():
    """
    进程池初始化函数：每个子进程启动时会运行一次。
    在这里初始化 PaddleOCR，避免每次处理图片都重新加载模型。
    """
    global _worker_ocr
    # 为了防止子进程抢占 CPU 资源导致死锁或变慢，通常建议限制 OpenMP 线程数
    os.environ["OMP_NUM_THREADS"] = "1" 
    
    # 初始化 OCR (根据你的需求调整参数)
    _worker_ocr = PaddleOCR(use_textline_orientation=True, lang="ch")

def parse_ocr_result(raw_result):
    """
    辅助函数：清洗 PaddleOCR 数据 (保持你原有的逻辑不变)
    """
    clean_data = []
    if not raw_result or raw_result[0] is None:
        return clean_data

    first_item = raw_result[0]

    # 分支 A: 新版 PaddleX 格式 (字典)
    if isinstance(first_item, dict):
        texts = first_item.get('rec_texts', [])
        scores = first_item.get('rec_scores', [])
        boxes = first_item.get('dt_polys', [])
        for i in range(len(texts)):
            box = boxes[i] if i < len(boxes) else []
            if isinstance(box, np.ndarray): box = box.tolist()
            score = scores[i] if i < len(scores) else 1.0
            if isinstance(score, (np.float32, np.float64)): score = float(score)
            clean_data.append({"box": box, "text": texts[i], "score": score})

    # 分支 B: 经典列表格式
    elif isinstance(first_item, list):
        for line in first_item:
            box = line[0]
            if isinstance(box, np.ndarray): box = box.tolist()
            content = line[1]
            if isinstance(content, (list, tuple)):
                text, score = content[0], content[1]
            else:
                text, score = content, 1.0
            if isinstance(score, (np.float32, np.float64)): score = float(score)
            clean_data.append({"box": box, "text": text, "score": score})
            
    return clean_data

def process_page_task(img_np):
    """
    子进程执行的任务函数
    接收 numpy 图片数组，返回清洗后的数据
    """
    global _worker_ocr
    try:
        # 调用子进程内的全局 OCR 实例
        raw_result = _worker_ocr.ocr(img_np)
        return parse_ocr_result(raw_result)
    except Exception as e:
        print(f"Worker Error: {str(e)}")
        return []

# ==========================================
# Flask 路由
# ==========================================

@app.route('/ocr', methods=["POST"])
def paddle_ocr():
    try:
        if not request.data:
            return jsonify(code=400, message="没有接收到数据"), 400
            
        data = json.loads(request.data)
        img_url = data.get("imgUrl")

        if not img_url:
            return jsonify(code=400, message="imgUrl 参数缺失"), 400

        print(f"正在处理: {img_url}")
        
        # 准备图片列表 (List of numpy arrays)
        image_arrays = []

        # 1. 资源加载与转换
        if img_url.lower().endswith('.pdf'):
            try:
                images = []
                if img_url.startswith('http://') or img_url.startswith('https://'):
                    resp = requests.get(img_url)
                    if resp.status_code == 200:
                        images = convert_from_bytes(resp.content)
                    else:
                        return jsonify(code=400, message="无法下载 PDF 文件"), 400
                else:
                    images = convert_from_path(img_url)
                
                # 将 PIL Image 预先转换为 Numpy Array，方便传递给子进程
                # 注意：如果 PDF 极大，这步可能会占大量内存，可考虑分批
                image_arrays = [np.array(img) for img in images]
                print(f"PDF 加载成功，共 {len(image_arrays)} 页")
                
            except Exception as pdf_err:
                return jsonify(code=500, message=f"PDF 解析失败: {str(pdf_err)}"), 500
        else:
            # 单张图片处理
            # PaddleOCR.ocr() 支持路径，但为了统一并行逻辑，我们最好自己读图或者让 PaddleOCR 内部处理
            # 这里为了复用 process_page_task，我们简单处理：
            # 如果是单图，并行意义不大，但为了代码结构统一，也可以放进去，或者保留原逻辑。
            # 这里演示统一逻辑：虽然只有一张图，也走 ProcessPool
            # 注意：PaddleOCR 内部对 url 处理比较复杂，如果 img_url 是网络图片，
            # 建议先下载转 numpy，或者让 worker 直接处理 path。
            # 简单起见，这里假设单图并发需求低，直接走原逻辑或作为单元素列表。
            
            # 为了简单，单图我们这里暂不走复杂的 numpy 转换，直接让 worker 处理路径
            # 但 worker 接收的是 img_np。所以这里特殊处理一下单图逻辑，
            # 或者你可以选择只对 PDF 并行。
            
            # 策略：单图直接在主进程做（省去进程开销），多页 PDF 并行。
            print("单图模式，直接处理...")
            # 临时实例化一个（或者在全局保留一个单例用于单图）
            # 为了演示方便，这里我们假设单图也用 Pool (实际上单图用 Pool 开销比收益大)
            # 生产环境建议：单图直接调全局，PDF 调 Pool。
            pass 

        # 2. 执行识别 (核心并行部分)
        final_data = []
        
        if img_url.lower().endswith('.pdf') and len(image_arrays) > 0:
            # 确定并行数量：CPU 核心数 或 图片页数 取小
            max_workers = min(os.cpu_count(), len(image_arrays))
            print(f"启动并行 OCR，使用进程数: {max_workers}")

            with ProcessPoolExecutor(max_workers=max_workers, initializer=init_worker) as executor:
                # map 会按照输入顺序返回结果，所以页码顺序是保序的
                results = list(executor.map(process_page_task, image_arrays))
                final_data = results
        else:
            # 非 PDF 或 单图逻辑 (使用主进程的 OCR 或 临时新建)
            # 为了代码健壮，这里使用简单的非并行回退逻辑
            # 注意：你需要确保主进程也有一个 ocr 实例，或者在此时创建
            temp_ocr = PaddleOCR(lang="ch")
            raw_result = temp_ocr.ocr(img_url)
            final_data.append(parse_ocr_result(raw_result))

        # 3. 统计结果
        total_items = sum(len(page) for page in final_data)
        print(f"识别结束，共 {len(final_data)} 页，合计 {total_items} 条文本")

        return jsonify(data=final_data, code=200, message="调用接口成功"), 200

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify(data=None, code=500, message=f"服务器内部错误: {str(e)}"), 500

if __name__ == '__main__':
    # Windows 下使用多进程必须放在 if __name__ == '__main__': 之下
    # Linux 下通常也建议这么做
    app.config['JSON_AS_ASCII'] = False
    
    # 也可以在启动 App 前预热一个全局 ProcessPoolExecutor，
    # 但 Flask 的 debug=True reloader 可能会导致进程池异常，生产环境建议配合 Gunicorn 使用。
    app.run(host='0.0.0.0', debug=False, port=8888) 
    # 注意：Debug模式在多进程下有时会怪异，建议设为 False 测试并行