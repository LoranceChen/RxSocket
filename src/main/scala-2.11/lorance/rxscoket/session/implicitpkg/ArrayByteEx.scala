package lorance.rxscoket.session.implicitpkg

import java.nio.charset.StandardCharsets

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

  def string = {
    new String(ab, StandardCharsets.UTF_8)
  }
}
