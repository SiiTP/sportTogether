//package service
//
//import java.security.cert.PKIXRevocationChecker.Option
//import java.util.Calendar
//
//import dao.AccountDAO
//import org.scalatest._
//
//class AccountServiceTest extends FlatSpec with Matchers {
//  "Account Service" should "check authorization status" in {
//    val accountService = new AccountService(new AccountDAO)
//    accountService.isAuthorized("q") should be (AccountService.CODE_NOT_AUTHORIZED)
//  }
//
//  it should "register not exist account" in {
//    val accountService = new AccountService(new AccountDAO)
//    accountService.register("someSession", "acc", null, null) shouldEqual AccountService.CODE_REG_SUCCESSFUL
//  }
//
//  it should "not register exist account" in {
//    val accountService = new AccountService(new AccountDAO)
//    accountService.register("someSession", "acc", null, null)
//    accountService.register("someSession", "acc", null, null) shouldEqual AccountService.CODE_REG_ACC_EXIST
//  }
//
//  it should "automatically authorize registered account" in {
//    val accountService = new AccountService(new AccountDAO)
//    val session: String = "someSession"
//    accountService.register(session, "acc", null, null)
//    accountService.isAuthorized(session) shouldEqual AccountService.CODE_AUTHORIZED
//  }
//
//  it should "authorize unauthorized account" in {
//    val accountDAO = new AccountDAO
//    val name = "acc"
//    val password = "pass"
//    accountDAO.create(name, password, null, null, 0)
//    val accountService = new AccountService(accountDAO)
//    val session = "someSession"
//    accountService.authorize(session, name, password, AccountService.timeoutNextWeek()) shouldEqual AccountService.CODE_AUTH_SUCCESSFUL
//    accountService.isAuthorized(session) shouldEqual AccountService.CODE_AUTHORIZED
//  }
//
//  it should "not authorize account with wrong password" in {
//    val accountDAO = new AccountDAO
//    val name = "acc"
//    val password = "pass"
//    accountDAO.create(name, password, null, null, 0)
//    val accountService = new AccountService(accountDAO)
//    val session = "someSession"
//    accountService.authorize(session, name, "wrong password", AccountService.timeoutNextWeek()) shouldEqual AccountService.CODE_AUTH_UNSUCCESSFUL
//    accountService.isAuthorized(session) shouldEqual AccountService.CODE_NOT_AUTHORIZED
//  }
//
//  it should "not authorize account twice" in {
//    val accountDAO = new AccountDAO
//    val name = "acc"
//    val password = "pass"
//    accountDAO.create(name, password, null, null, 0)
//    val accountService = new AccountService(accountDAO)
//    val session = "someSession"
//    accountService.authorize(session, name, password, AccountService.timeoutNextWeek()) shouldEqual AccountService.CODE_AUTH_SUCCESSFUL
//    accountService.authorize(session, name, password, AccountService.timeoutNextWeek()) shouldEqual AccountService.CODE_AUTH_ALREADY
//  }
//
//  it should "set right timeout of authorization in timeout hashmap" in {
//    val accountDAO = new AccountDAO
//    accountDAO.create("acc", "pass", null, null, 0)
//    val accountService = new AccountService(accountDAO)
//    val session = "someSession"
//    val timeout = AccountService.timeoutNextWeek()
//    accountService.authorize(session, "acc", "pass", timeout)
//    accountService.sessionTimeout().get(session) shouldEqual timeout
//  }
//
//  it should "unauthorize accounts with passed timeout" in {
//    val accountDAO = new AccountDAO
//    accountDAO.create("acc", "pass", null, null, 0)
//    val accountService = new AccountService(accountDAO)
//    val session = "someSession"
//    val timeout = Calendar.getInstance().getTimeInMillis - 1
//    accountService.authorize(session, "acc", "pass", timeout)
//    accountService.isAuthorized(session) shouldEqual AccountService.CODE_NOT_AUTHORIZED_TIMEOUT
//  }
//
//  it should "erase maps when unauthorize" in {
//    val accountDAO = new AccountDAO
//    val name = "acc"
//    val password = "pass"
//    accountDAO.create(name, password, null, null, 0)
//    val accountService = new AccountService(accountDAO)
//    val session = "someSession"
//    val timeout = Calendar.getInstance().getTimeInMillis - 1
//    accountService.authorize(session, name, password, timeout)
//    accountService.unAuthorize(session)
//    accountService.authAccounts() should have size 0
//    accountService.sessionTimeout() should have size 0
//  }
//
//  it should "authorize again after unauthorize" in {
//    val accountDAO = new AccountDAO
//    val accountService = new AccountService(accountDAO)
//    val name = "acc"
//    val password = "pass"
//    val session = "someSession"
//    accountService.registerUser(session, name, password)
//    accountService.unAuthorize(session).isDefined shouldEqual true
//    accountService.authorize(session, name, "wrong password", AccountService.timeoutNextWeek()) shouldEqual AccountService.CODE_AUTH_UNSUCCESSFUL
//    accountService.authorize(session, name, password, AccountService.timeoutNextWeek()) shouldEqual AccountService.CODE_AUTH_SUCCESSFUL
//  }
//
//  it should "get account" in {
//    val accountDAO = new AccountDAO
//    val accountService = new AccountService(accountDAO)
//    val name = "acc"
//    val password = "pass"
//    val session = "someSession"
//    accountService.registerUser(session, name, password)
//    val maybeAccount = accountService.getAccount(session)
//    maybeAccount.get.name shouldEqual name
//  }
//
//  it should "get empty option when no acount" in {
//    val accountService = new AccountService(new AccountDAO)
//    accountService.getAccount("someSession")
//  }
//}
