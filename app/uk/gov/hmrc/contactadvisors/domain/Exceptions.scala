package uk.gov.hmrc.contactadvisors.domain

final case class CustomerIsNotPaperless(reason: String) extends Exception(reason)
final case class TaxIdNotFound(reason: String) extends Exception(reason)
//
final case class MessageAlreadyExists(reason: String) extends Exception(reason)
final case class UnexpectedFailure(reason: String) extends Exception(reason)
