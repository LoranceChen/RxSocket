package lorance.rxscoket.session.implicitpkg

/**
  *
  */
class IntEx(int: Int) {
  //enCode byte to Array[Byte]
  def getByteArray = {
    val bytes = new Array[Byte](4)

    //assume int is Big Endian
    bytes(0) = ((int >> 24) & 0xFF).toByte
    bytes(1) = ((int >> 16) & 0xFF).toByte
    bytes(2) = ((int >> 8) & 0xFF).toByte
    bytes(3) = (int & 0xFF).toByte
    bytes
  }
}
