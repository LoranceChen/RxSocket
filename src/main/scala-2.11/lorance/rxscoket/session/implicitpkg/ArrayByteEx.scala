package lorance.rxscoket.session.implicitpkg

/**
  *
  */
class ArrayByteEx(ab: Array[Byte]) {
  def toInt = {
    ab(3) & 0xFF |
    (ab(2) & 0xFF) << 8 |
    (ab(1) & 0xFF) << 16 |
    (ab(0) & 0xFF) << 24
  }
}
