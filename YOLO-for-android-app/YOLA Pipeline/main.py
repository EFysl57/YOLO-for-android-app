from fastapi import FastAPI, UploadFile, File
from ultralytics import YOLO
import cv2
import numpy as np
import torch

app = FastAPI()

model = YOLO("yolo26n.pt")

@app.post("/detect")
async def detect(image: UploadFile = File(...)):
    image_bytes = await image.read()

    img = cv2.imdecode(
        np.frombuffer(image_bytes, np.uint8),
        cv2.IMREAD_COLOR
    )

    results = model(img)[0]

    names = model.names

    detections = []

    for box in results.boxes:
        cls = int(box.cls[0])
        detections.append({
            "class_id": cls,
            "class_name": names[cls],
            "confidence": float(box.conf[0]),
            "box": box.xyxy[0].tolist()
        })

    return {"detections": detections}