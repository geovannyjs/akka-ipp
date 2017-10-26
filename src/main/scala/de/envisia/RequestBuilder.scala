package de.envisia

import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

import akka.util.ByteString

final class IppRequest(val request: ByteString) extends AnyVal

class RequestBuilder[Request <: RequestBuilder.Request](
    attributes: Map[String, (Byte, String)] = Map.empty[String, (Byte, String)]
) {

  import de.envisia.RequestBuilder.Request._

  implicit val bO: ByteOrder = ByteOrder.BIG_ENDIAN

  /**
    * common setters
    */
  def setCharset(charset: String): RequestBuilder[Request with Charset] =
    new RequestBuilder(attributes + ("attributes-charset" -> (0x47.toByte, charset)))

  def setUri(uri: String): RequestBuilder[Request with PrinterUri] =
    new RequestBuilder(attributes + ("printer-uri" -> (0x45.toByte, uri)))

  def setLanguage(lang: String): RequestBuilder[Request with Language] =
    new RequestBuilder(attributes + ("attributes-natural-language" -> (0x48.toByte, lang)))

  def setUser(user: String): RequestBuilder[Request with User] =
    new RequestBuilder(attributes + ("requesting-user-name" -> (0x42.toByte, user)))

  def setJobName(jobName: String): RequestBuilder[Request with JobName] =
    new RequestBuilder(attributes + ("job-name" -> (0x42.toByte, jobName)))

  def setFormat(format: String): RequestBuilder[Request with Format] =
    new RequestBuilder(attributes + ("document-format" -> (0x49.toByte, format)))

  def askWithJobId(jobId: Int): RequestBuilder[Request with JobId] =
    new RequestBuilder(attributes + ("job-id" -> (0x21.toByte, jobId.toString)))

  /**
    *  more general setters
    */
  def addOperationAttribute(tag: Byte, name: String, value: String): RequestBuilder[Request with OperationAttribute] =
    new RequestBuilder[Request with OperationAttribute](attributes + (name -> (tag, value)))

  def addJobAttribute(tag: Byte, name: String, value: String): RequestBuilder[Request with JobAttribute] =
    new RequestBuilder[Request with JobAttribute](attributes + (name -> (tag, value)))

  //http://tools.ietf.org/html/rfc2910#section-3.1.1
  //	-----------------------------------------------
  //	|                  version-number             |   2 bytes  - required
  //	-----------------------------------------------
  //	|               operation-id (request)        |
  //	|                      or                     |   2 bytes  - required
  //	|               status-code (response)        |
  //	-----------------------------------------------
  //	|                   request-id                |   4 bytes  - required
  //	-----------------------------------------------
  //	|                 attribute-group             |   n bytes - 0 or more
  //	-----------------------------------------------
  //	|              end-of-attributes-tag          |   1 byte   - required
  //	-----------------------------------------------
  //	|                     data                    |   q bytes  - optional
  //	-----------------------------------------------

  // generic byte strings
  @inline protected final def putHeader(operationId: Byte, requestId: Int): ByteString =
    ByteString.newBuilder
      .putBytes(Array(0x02.toByte, 0x00.toByte))
      .putBytes(Array(0x00.toByte, operationId))
      .putInt(requestId) // TODO does not increment
      .putByte(Constants.ATTRIBUTE_GROUPS("operation-attributes-tag"))
      .result()
  @inline protected final def putAttribute(name: String): ByteString =
    ByteString.newBuilder
      .putByte(attributes(name)._1)
      .putShort(name.length)
      .putBytes(name.getBytes(StandardCharsets.UTF_8))
      .putShort(attributes(name)._2.length)
      .putBytes(attributes(name)._2.getBytes(StandardCharsets.UTF_8))
      .result()
  @inline protected val putEnd: ByteString =
    ByteString.newBuilder
      .putByte(Constants.ATTRIBUTE_GROUPS("end-of-attributes-tag"))
      .result()

  def buildGetPrinterAttr(operationId: Byte, requestId: Int)(
      implicit ev: Request =:= GetPrinterAttributes
  ): IppRequest = new IppRequest(
    putHeader(operationId, requestId)
      ++ putAttribute("attributes-charset")
      ++ putAttribute("attributes-natural-language")
      ++ putAttribute("printer-uri") ++ putEnd
  )

  def buildPrintJob(operationId: Byte, requestId: Int)(implicit ev: Request =:= PrintJob): IppRequest = new IppRequest(
    putHeader(operationId, requestId)
      ++ putAttribute("attributes-charset")
      ++ putAttribute("attributes-natural-language")
      ++ putAttribute("printer-uri")
      ++ putAttribute("requesting-user-name")
      ++ putAttribute("job-name")
      ++ putAttribute("document-format")
      ++ putEnd
  )

  def buildValidateJob(operationId: Byte, requestId: Int)(implicit ev: Request =:= ValidateJob): IppRequest =
    new IppRequest(
      putHeader(operationId, requestId)
        ++ putAttribute("attributes-charset")
        ++ putAttribute("attributes-natural-language")
        ++ putAttribute("printer-uri")
        ++ putAttribute("requesting-user-name")
        ++ putAttribute("job-name")
        ++ putAttribute("document-format")
        ++ putEnd
    )

  def buildGetJobAttr(operationId: Byte, requestId: Int)(implicit ev: Request =:= GetJobAttributes): IppRequest =
    new IppRequest(
      putHeader(operationId, requestId)
        ++ putAttribute("attributes-charset")
        ++ putAttribute("attributes-natural-language")
        ++ putAttribute("printer-uri")
        ++ putAttribute("job-id")
        ++ putAttribute("requesting-user-name")
      // ++ putAttribute("requested-attributes") // optionally https://tools.ietf.org/html/rfc2911#section-3.2.5.1
        ++ putEnd
    )

}

object RequestBuilder {

  sealed trait Request

  object Request {

    sealed trait EmptyRequest       extends Request
    sealed trait Charset            extends Request
    sealed trait Language           extends Request
    sealed trait PrinterUri         extends Request
    sealed trait User               extends Request
    sealed trait JobName            extends Request
    sealed trait Format             extends Request
    sealed trait JobAttribute       extends Request
    sealed trait OperationAttribute extends Request
    sealed trait JobId              extends Request

    //type MinimalRequest = EmptyRequest
    type GetPrinterAttributes = EmptyRequest with Charset with Language with PrinterUri
    type PrintJob             = EmptyRequest with Charset with Language with PrinterUri with User with JobName with Format
    type ValidateJob          = EmptyRequest
    type GetJobAttributes     = EmptyRequest with Charset with Language with PrinterUri with User with JobId

  }

}