package lorance.rxsocket.session

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

case class Attachment(byteBuffer: ByteBuffer, socketChannel: AsynchronousSocketChannel)