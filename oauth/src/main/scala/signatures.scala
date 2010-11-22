package unfiltered.oauth

object Signatures {
  import OAuth._
  
  type Params = Map[String, Seq[String]]
  
  val signatures = Map("HMAC-SHA1" -> HmacSha1)
  
  implicit val encoding = "UTF-8"
  
  trait ParamNormalizing {
    /** http://tools.ietf.org/html/rfc5849#section-3.4.1.3 */
    def normalizedParams(p: Params) =
      (new collection.immutable.TreeMap[String, Seq[String]] ++ p) map { case (k, v) => v.toList.sort (_<_) map { 
          case e => k + "=" + e  } mkString "&" }  mkString "&"
  }
  
  trait Signature extends Encoding with ParamNormalizing with Combining {
    def sign(method: String, url: String, params: Params, consumerSec: String, tokenSec: String): String
  }
  
  object HmacSha1 extends Signature {
    import javax.crypto
    
    val SHA1 = "HmacSHA1"
    def sign(method: String, url: String, p: Params, consumerSec: String, tokenSec: String) = {
      val keyStr = combine(consumerSec, tokenSec)
      val baseStr = combine(method, url, normalizedParams(p - Sig))
      val sig = {
        val mac = crypto.Mac.getInstance(SHA1)
        mac.init(new crypto.spec.SecretKeySpec(bytes(keyStr), SHA1))
        new String(base64Encode(mac.doFinal(bytes(baseStr))))
      }
      sig
    }
  }
  
  def verify(method: String, url: String, p: Params, consumerSec: String, tokenSec: String) =
    (for {
      sig <- signatures.get(p(SignatureMethod)(0))
    } yield {
      val expected = sig.sign(method.toUpperCase, url, p, consumerSec, tokenSec)
      val actual = Encoding.decode(p(Sig)(0))
      expected == actual
    }) getOrElse(false)
}