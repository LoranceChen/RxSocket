package lorance.rxscoket.session.implicitpkg

/**
  *
  */
class IntEx(int: Int) {
  //enCode byte to Array[Byte]
  def getByteArray = {
    val bytes = new Array[Byte](4)
    //Big Endian
    bytes(1) = ((int >> 24) & 0xFF).toByte
    bytes(2) = ((int >> 16) & 0xFF).toByte
    bytes(3) = ((int >> 8) & 0xFF).toByte
    bytes(4) = (int & 0xFF).toByte
    bytes
  }
}
