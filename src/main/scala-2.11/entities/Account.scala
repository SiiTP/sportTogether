package entities

/**
  * Created by root on 11.07.16.
  */


class Account(val name : String, val password : String, val role : String, val session : String, timeout: Long) {

  def this(name : String, password : String, role : String) = this(name, password, role, null, 0)

  override def equals(o: Any) = o match {
    case that: Account => that.name == this.name
    case _ => false
  }

  override def hashCode(): Int = name.hashCode()
}

object Account {
  val ROLE_ADMIN = "admin"
  val ROLE_USER = "user"
}
