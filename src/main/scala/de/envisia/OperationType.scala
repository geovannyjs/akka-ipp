package de.envisia

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future

sealed abstract class OperationType(val name: String, val operationId: Byte)

case object GetPrinterAttributes
    extends OperationType("Get-Printer-Attributes", Constants.OPERATION_IDS("Get-Printer-Attributes"))

case class PrintJob(file: Source[ByteString, Future[IOResult]])
    extends OperationType("Print-Job", Constants.OPERATION_IDS("Print-Job"))

case object ValidateJob extends OperationType("Validate-Job", Constants.OPERATION_IDS("Validate-Job"))

case object GetJobAttributes extends OperationType("Get-Job-Attributes", Constants.OPERATION_IDS("Get-Job-Attributes"))