package com.example.image_server2

import java.io.File
import java.net.Socket
import java.nio.ByteBuffer

/**
 * Cliente de rede responsável pelo envio de imagens para o servidor.
 * Implementa protocolo simples onde primeiro envia o tamanho da imagem
 * seguido pelos bytes da imagem.
 */
object NetworkClient {

    /**
     * Envia uma imagem para o servidor especificado.
     * @param file Arquivo de imagem a ser enviado
     * @param host Endereço IP do servidor
     * @param port Porta do servidor
     * @throws Exception em caso de erro de rede ou I/O
     */
    fun sendImage(file: File, host: String, port: Int) {
        val socket = Socket(host, port)
        val output = socket.getOutputStream()

        val bytes = file.readBytes()

        // Envia tamanho da imagem (4 bytes)
        val sizeBuffer = ByteBuffer.allocate(4).putInt(bytes.size).array()
        output.write(sizeBuffer)

        // Envia imagem
        output.write(bytes)
        output.flush()
        socket.close()
    }
}
