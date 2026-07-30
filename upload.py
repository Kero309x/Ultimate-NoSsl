import requests
import json

url = "https://bashupload.com/app-debug.apk"
with open("app/build/outputs/apk/debug/app-debug.apk", "rb") as f:
    response = requests.put(url, data=f)
print(response.text)
