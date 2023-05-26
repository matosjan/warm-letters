from http.server import HTTPServer, BaseHTTPRequestHandler

from io import BytesIO
import cv2 as cv
import numpy as np
from scanner import Scanner
import yaml

from requests_toolbelt.multipart import decoder

from time import sleep


class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b'Hello, world!')

    def do_POST(self):
        self.path = '/upload'

        content_type = self.headers['Content-Type']
        content_length = int(self.headers['Content-Length'])

        file_content = self.rfile.read(content_length)

        image_bytes = file_content

        if 'multipart/form-data' in content_type:
            multipart_data = decoder.MultipartDecoder(file_content, content_type).parts
            image_bytes = multipart_data[0].content
        elif 'image/jpeg' in content_type:
            image_bytes = file_content
        else:
            print("Error: content_type is not recognized")
            exit()
        
        image_numpy = np.frombuffer(image_bytes, np.int8)
        im = cv.imdecode(image_numpy, cv.IMREAD_UNCHANGED)
        scanner = Scanner()
        inp = scanner.scan(im) # result from first block
        self.send_response(200)
        self.end_headers()
        print('Success')

from os.path import abspath
config_path = '../app/src/main/assets/config/config.yaml'
try:
    with open(config_path) as f:
        config = yaml.load(f, Loader=yaml.FullLoader)
except FileNotFoundError:
    print(f"Error: config file {abspath(config_path)} not found.\nPlease add a config file")
    exit()

ip_address = config['ip_address']
port = int(config['port'])

httpd = HTTPServer((ip_address, port), SimpleHTTPRequestHandler)
print(f"Server running at http://{ip_address}:{port}/")

httpd.serve_forever()
