package entities

import org.scalatest._

import scala.collection.mutable

/**
  * Created by root on 13.07.16.
  */
class HashSetTest extends FlatSpec with Matchers {

  "HashSet" should "add elements correctly" in {
    val accountSet = new mutable.HashSet[Account]()
    val account = new Account("1", "", "")

    accountSet += account

    accountSet.contains(account) should be (true)
  }

  it should "not add the same elements" in {
    val accountSet = new mutable.HashSet[Account]()
    val account1 = new Account("first", "1", "1")
    val account2 = new Account("second", "2", "2")
    val account3 = new Account("first", "3", "3")

    accountSet += account1
    accountSet += account2
    accountSet += account3

    accountSet.size should be (2)
  }

  it should "contain only added elements" in {
    val accountSet = new mutable.HashSet[Account]()
    val account1 = new Account("first", "1", "1")
    val account2 = new Account("second", "2", "2")
    val account3 = new Account("third", "3", "3")

    accountSet += account1
    accountSet += account2

    accountSet should contain allOf (account1, account2)
    accountSet should not contain account3
  }

  it should "remove elements correctly" in {
    val accountSet = new mutable.HashSet[Account]()
    val account = new Account("first", "1", "1")

    accountSet += account
    accountSet should contain (account)

    accountSet -= account
    accountSet should not contain account
  }

  it should "update element correctly" in {
    val accountSet = new mutable.HashSet[Account]()
    val account = new Account("first", "1", "1")

    accountSet.update(account, true)
    accountSet.size should be (1)

    accountSet.update(account, false)
    accountSet.size should be (0)
  }

}
