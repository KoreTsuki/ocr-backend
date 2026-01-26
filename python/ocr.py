import json
import numpy as np
from paddleocr import PaddleOCR
from flask import Flask, request, jsonify

# 初始化 OCR
ocr = PaddleOCR(use_textline_orientation=True, lang="ch")

app = Flask(__name__)

@app.route('/ocr', methods=["POST"])
def paddle_ocr():
    try:
        # 1. 接收参数
        if not request.data:
            return jsonify(code=400, message="没有接收到数据"), 400
            
        data = json.loads(request.data)
        img_url = data.get("imgUrl")

        if not img_url:
            return jsonify(code=400, message="imgUrl 参数缺失"), 400

        print(f"正在处理图片: {img_url}")

        # 2. 调用 PaddleOCR
        raw_result = ocr.ocr(img_url)

        # 3. 数据清洗与提取
        clean_data = []

        if raw_result:
            first_item = raw_result[0]

            # ==========================================
            # 分支 A: 新版 PaddleX 格式 (字典)
            # ==========================================
            if isinstance(first_item, dict):
                print("检测到新版字典格式，正在提取数据...")
                
                texts = first_item.get('rec_texts', [])
                scores = first_item.get('rec_scores', [])
                boxes = first_item.get('dt_polys', [])

                for i in range(len(texts)):
                    # 提取坐标
                    box = boxes[i] if i < len(boxes) else []
                    if isinstance(box, np.ndarray):
                        box = box.tolist()
                    
                    # 提取分数
                    score = scores[i] if i < len(scores) else 1.0
                    if isinstance(score, (np.float32, np.float64)):
                        score = float(score)

                    clean_data.append({
                        "box": box,
                        "text": texts[i],
                        "score": score
                    })

            # ==========================================
            # 分支 B: 经典列表格式 (List of lists)
            # ==========================================
            elif isinstance(first_item, list):
                print("检测到经典列表格式，正在提取数据...")
                for line in first_item:
                    box = line[0]
                    if isinstance(box, np.ndarray):
                        box = box.tolist()
                    
                    content = line[1]
                    if isinstance(content, (list, tuple)):
                        text = content[0]
                        score = content[1]
                    else:
                        text = content
                        score = 1.0
                    
                    if isinstance(score, (np.float32, np.float64)):
                        score = float(score)

                    clean_data.append({
                        "box": box,
                        "text": text,
                        "score": score
                    })

        # 4. 打印结果验证
        if clean_data:
            print(f"识别成功，共 {len(clean_data)} 行。第一行: {clean_data[0]['text']}")
        else:
            print("识别结果为空")

        # 【核心修改点】
        # Java端期望的是 [[{...}]] 结构，所以这里我们要把 clean_data 再包一层列表
        final_data = [clean_data] 

        return jsonify(data=final_data, code=200, message="调用接口成功"), 200

    except Exception as e:
        print("======== 发生错误 ========")
        import traceback
        traceback.print_exc()
        return jsonify(data=None, code=500, message=f"服务器内部错误: {str(e)}"), 500

if __name__ == '__main__':
    app.config['JSON_AS_ASCII'] = False
    app.run(host='0.0.0.0', debug=True, port=8888)