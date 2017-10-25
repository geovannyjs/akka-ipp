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

  /**
    *  more general setters
    */
  def addOperationAttribute(tag: Byte, name: String, value: String): RequestBuilder[Request with OperationAttribute] =
    new RequestBuilder[Request with OperationAttribute](attributes + (name -> (tag, value)))

  def addJobAttribute(tag: Byte, name: String, value: String): RequestBuilder[Request with JobAttribute] =
    new RequestBuilder[Request with JobAttribute](attributes + (name -> (tag, value)))

  // generic byte strings
  @inline protected final def putHeader(operationId: Byte): ByteString =
    ByteString.newBuilder
      .putBytes(Array(0x02.toByte, 0x00.toByte))
      .putBytes(Array(0x00.toByte, operationId))
      .putInt(1)
      .putByte(0x01.toByte) // start operation group
      .result()
  @inline protected final def putAttribute(tag: Byte, name: String, value: String): ByteString =
    ByteString.newBuilder
      .putByte(tag)
      .putShort(name.length)
      .putBytes(name.getBytes(StandardCharsets.UTF_8))
      .putShort(value.length)
      .putBytes(value.getBytes(StandardCharsets.UTF_8))
      .result()
  @inline protected val putEnd: ByteString =
    ByteString.newBuilder
      .putByte(0x03.toByte) // stop operation group
      .result()

  def buildGetPrinterAttr(implicit ev: Request =:= GetPrinterAttributes): IppRequest = new IppRequest(
    putHeader(0x0b.toByte) ++ putAttribute(
      attributes("attributes-charset")._1,
      "attributes-charset",
      attributes("attributes-charset")._2
    )
      ++ putAttribute(
        attributes("attributes-natural-language")._1,
        "attributes-natural-language",
        attributes("attributes-natural-language")._2
      )
      ++ putAttribute(attributes("printer-uri")._1, "printer-uri", attributes("printer-uri")._2) ++ putEnd
  )

  def buildPrintJob(implicit ev: Request =:= PrintJob): IppRequest = new IppRequest(
    putHeader(0x02.toByte) ++ putAttribute(
      attributes("attributes-charset")._1,
      "attributes-charset",
      attributes("attributes-charset")._2
    )
      ++ putAttribute(
        attributes("attributes-natural-language")._1,
        "attributes-natural-language",
        attributes("attributes-natural-language")._2
      )
      ++ putAttribute(attributes("printer-uri")._1, "printer-uri", attributes("printer-uri")._2)
      ++ putAttribute(
        attributes("requesting-user-name")._1,
        "requesting-user-name",
        attributes("requesting-user-name")._2
      )
      ++ putAttribute(attributes("job-name")._1, "job-name", attributes("job-name")._2)
      ++ putAttribute(attributes("document-format")._1, "document-format", attributes("document-format")._2)
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

    //type MinimalRequest = EmptyRequest
    type GetPrinterAttributes = EmptyRequest with Charset with Language with PrinterUri
    type PrintJob             = EmptyRequest with Charset with Language with PrinterUri with User with JobName with Format
    type ValidateJob          = EmptyRequest

  }

}