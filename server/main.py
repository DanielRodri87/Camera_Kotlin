import socket
import struct
import threading
import tkinter as tk
from tkinter import scrolledtext
from PIL import Image, ImageTk, ImageDraw
import threading
import io

HOST = "0.0.0.0"
PORT = 4400
image_counter = 0  # Contador global de imagens

# Função para receber a imagem via socket
def receive_image(conn, frontend_callback):
    data = conn.recv(4)
    if not data:
        return None
    img_size = struct.unpack("!I", data)[0]
    frontend_callback(f"[INFO] Tamanho da imagem recebido: {img_size} bytes\n")

    received = b""
    while len(received) < img_size:
        packet = conn.recv(4096)
        if not packet:
            break
        received += packet

    if len(received) != img_size:
        frontend_callback("[ERRO] Imagem incompleta recebida.\n")
        return None

    frontend_callback(f"[OK] Imagem recebida com sucesso!\n")
    return received

# Função que roda o servidor em thread separada
def server_thread(frontend_callback, image_callback):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
        server.bind((HOST, PORT))
        server.listen(5)
        frontend_callback(f" Aguardando Imagem\n")

        while True:
            conn, addr = server.accept()
            frontend_callback(f"[CONECTADO] Cliente {addr}\n")
            with conn:
                img_data = receive_image(conn, frontend_callback)
                if img_data:
                    # Converte bytes para imagem sem salvar em disco
                    image = Image.open(io.BytesIO(img_data))
                    image_callback(image)

# Funções de frontend
import tkinter as tk
from PIL import Image, ImageTk
import threading

import tkinter as tk
from PIL import Image, ImageTk
import threading

import tkinter as tk
from PIL import Image, ImageTk
import threading

import tkinter as tk
from PIL import Image, ImageTk, ImageDraw, ImageFilter
import threading
import random

import tkinter as tk
from PIL import Image, ImageTk, ImageDraw, ImageFilter
import threading
import random

import tkinter as tk
from PIL import Image, ImageTk, ImageDraw, ImageFilter
import threading
import random

def start_server():
    threading.Thread(target=server_thread, args=(log_message, display_image), daemon=True).start()

def log_message(msg):
    text_area.config(state=tk.NORMAL)
    text_area.delete("1.0", tk.END)
    text_area.insert(tk.END, msg)
    text_area.see(tk.END)
    text_area.config(state=tk.DISABLED)

def round_corners(image, radius=20):
    mask = Image.new("L", image.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0,0,image.size[0], image.size[1]), radius=radius, fill=255)
    rounded = Image.new("RGBA", image.size)
    rounded.paste(image, (0,0), mask=mask)
    return rounded

def wood_texture(size, base_color=(139,69,19)):
    img = Image.new("RGB", size, base_color)
    draw = ImageDraw.Draw(img)
    for i in range(0, size[0], 3):
        offset = random.randint(-8, 8)
        line_color = (
            min(255, base_color[0]+random.randint(-30,30)),
            min(255, base_color[1]+random.randint(-20,20)),
            min(255, base_color[2]+random.randint(-20,20))
        )
        draw.line((i, 0, i+offset, size[1]), fill=line_color, width=2)
    img = img.filter(ImageFilter.SMOOTH)
    return img

def add_frame(image, frame_width=25, radius=20):

    new_size = (image.width + 2*frame_width, image.height + 2*frame_width)
    frame = wood_texture(new_size)
    

    shadow = Image.new("RGBA", new_size, (0,0,0,0))
    shadow_draw = ImageDraw.Draw(shadow)
    shadow_draw.rounded_rectangle(
        [5,5,new_size[0]-5,new_size[1]-5], 
        radius=radius, fill=(0,0,0,80)
    )
    frame = Image.alpha_composite(shadow, frame.convert("RGBA"))
    

    rounded_image = round_corners(image, radius)
    
    # Cola a imagem no centro da moldura
    frame.paste(rounded_image, (frame_width, frame_width), mask=rounded_image)
    return frame

def display_image(image):
    global image_counter
    image_counter += 1
    
    # Salvar imagem com nome sequencial
    image_name = f"imagem_recebida{image_counter}.jpg"
    image.save(image_name)
    
    # Redimensionar para exibição
    image.thumbnail((600, 600))
    framed_image = add_frame(image, frame_width=25, radius=20)
    tk_image = ImageTk.PhotoImage(framed_image)
    
    image_label.config(image=tk_image)
    image_label.image = tk_image
    image_name_label.config(text=image_name)

# GUI Tkinter
root = tk.Tk()
root.title("Servidor de Imagem")
root.configure(bg="#F5F5DC")  # fundo bege

# Área de mensagens
text_area = tk.Text(
    root,
    width=50,
    height=3,
    state=tk.DISABLED,
    bg="#F5F5DC",
    fg="black",
    bd=0,
    highlightthickness=0,
)
text_area.pack(padx=10, pady=10, side="bottom")

# Label da imagem
image_label = tk.Label(root, bg="#F5F5DC")
image_label.pack(padx=10, pady=(10,0))

# Label do nome da imagem
image_name_label = tk.Label(root, text="", bg="#F5F5DC", fg="black")
image_name_label.pack(pady=(0,10))

log_message("Aguardando foto...\n")
start_server()

root.mainloop()