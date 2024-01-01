package me.dio.credit.application.system.service

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.enummeration.Status
import me.dio.credit.application.system.exception.BusinessException
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CustomerRepository
import me.dio.credit.application.system.service.impl.CreditService
import me.dio.credit.application.system.service.impl.CustomerService
import org.assertj.core.api.AssertDelegateTarget
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*

//@ActiveProfiles("test")
@ExtendWith(MockKExtension::class)
class CreditServiceTest {
    @MockK lateinit var customerService: CustomerService
    @MockK lateinit var creditRepository: CreditRepository
    @InjectMockKs lateinit var creditService: CreditService

    @Test
    fun `should create credit`() {
        //given
        val fakeCredit: Credit = buildCredit();
        val customer: Customer = buildCustomer()
        every { customerService.findById(any()) } returns fakeCredit.customer!!
        every { creditRepository.save(any()) } returns fakeCredit

        //when
        val actualCredit: Credit = creditService.save(fakeCredit)

        //expect
        Assertions.assertThat(actualCredit).isNotNull
        Assertions.assertThat(actualCredit).isSameAs(fakeCredit)
        verify (exactly = 1) {customerService.findById(customer.id!!)}
        verify (exactly = 1) {creditRepository.save(fakeCredit)}
    }

    @Test
    fun `should not create credit when firstInstallmentDay is beyond 3 months from now`() {

        //given
        val fakeCredit: Credit = buildCredit(dayFirstInstallment = LocalDate.now().plusMonths(5));
        every { customerService.findById(any()) } returns fakeCredit.customer!!
        every { creditRepository.save(any()) } returns fakeCredit

        //when
        //expect
        Assertions.assertThatThrownBy {
            creditService.save(fakeCredit)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage("Invalid Date")

        verify (exactly = 0) {creditRepository.save(any())}
    }

    @Test
    fun `should find credit by credit code`() {
        //given
        val fakeCredit: Credit = buildCredit();
        val customer: Customer = buildCustomer()
        every { customerService.findById(any()) } returns fakeCredit.customer!!
        every { creditRepository.findByCreditCode(any()) } returns fakeCredit

        //when
        val actualCredit: Credit = creditService.findByCreditCode(creditCode = fakeCredit.creditCode, customerId = customer.id!!)

        //expect
        Assertions.assertThat(actualCredit).isNotNull
        Assertions.assertThat(actualCredit).isSameAs(fakeCredit)
        verify (exactly = 1) {creditRepository.findByCreditCode(creditCode = fakeCredit.creditCode)}
    }

    @Test
    fun `should not find credit by unknown credit code`() {
        //given
        val fakeCredit: Credit = buildCredit();
        val customer: Customer = buildCustomer()
        every { creditRepository.findByCreditCode(any()) } returns null

        //when
        Assertions.assertThatThrownBy {
            creditService.findByCreditCode(creditCode = fakeCredit.creditCode, customerId = customer.id!!)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage("Creditcode ${fakeCredit.creditCode} not found")

        //expect
        verify (exactly = 1) {creditRepository.findByCreditCode(creditCode = fakeCredit.creditCode)}
    }

    @Test
    fun `should not find credit for invalid customer`() {
        //given
        val fakeCredit: Credit = buildCredit();
        val customer: Customer = buildCustomer(id = 2L)
        every { customerService.findById(any()) } returns customer
        every { creditRepository.findByCreditCode(any()) } returns fakeCredit

        //when
        Assertions.assertThatThrownBy {
            creditService.findByCreditCode(creditCode = fakeCredit.creditCode, customerId = customer.id!!)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Contact admin")

        //expect
        verify (exactly = 1) {creditRepository.findByCreditCode(creditCode = fakeCredit.creditCode)}
    }
    @Test
    fun `should return list of credits for a customer`() {
        //given
        val customerId:Long = 1L;
        val fakeCredit1: Credit = buildCredit(
            creditCode = UUID.randomUUID(),
            dayFirstInstallment = LocalDate.now().plusMonths(1));
        val fakeCredit2: Credit = buildCredit(
            creditCode = UUID.randomUUID(),
            dayFirstInstallment = LocalDate.now().plusMonths(2));
        fakeCredit1.customer?.id = customerId;
        fakeCredit2.customer?.id = customerId;

        val listOfCredits: List<Credit> = listOf(fakeCredit1, fakeCredit2)
        every { creditRepository.findAllByCustomerId(customerId) } returns listOfCredits

        //when
        val actualList: List<Credit> = creditService.findAllByCustomer(customerId)

        //expect
        Assertions.assertThat(actualList).isNotNull
        Assertions.assertThat(actualList).isNotEmpty
        Assertions.assertThat(actualList.size).isEqualTo(2)
        Assertions.assertThat(actualList).contains(fakeCredit1, fakeCredit2)
        verify (exactly = 1){ creditRepository.findAllByCustomerId(customerId) }
    }

    @Test
    fun `should not return list of credits for an unknown customer`() {
        //given
        val unknownCustomerId: Long = 2L;

        every { creditRepository.findAllByCustomerId(unknownCustomerId) } returns listOf<Credit>()

        //when
        val actualList: List<Credit> = creditService.findAllByCustomer(unknownCustomerId)

        //expect
        Assertions.assertThat(actualList).isNotNull
        Assertions.assertThat(actualList).isEmpty()
        Assertions.assertThat(actualList.size).isEqualTo(0)
        verify (exactly = 1){ creditRepository.findAllByCustomerId(unknownCustomerId) }
    }

    private fun buildCustomer(
        firstName: String = "Cami",
        lastName: String = "Cavalcante",
        cpf: String = "28475934625",
        email: String = "camial@gmail.com",
        password: String = "12345",
        zipCode: String = "123456",
        street: String = "Rua Cami",
        income: BigDecimal = BigDecimal.valueOf(1000.0),
        id: Long = 1L
    ) = Customer(
        firstName = firstName,
        lastName = lastName,
        cpf = cpf,
        email = email,
        password = password,
        address = Address(
            zipCode = zipCode,
            street = street,
        ),
        income = income,
        id = id,

        )

    private fun buildCredit(
        id: Long = 1L,
        customer: Customer = Customer(
            firstName = "Cami",
            lastName = "Cavalcante",
            cpf = "28475934625",
            email = "camial@gmail.com",
            password = "12345",
            address = Address(
                zipCode = "123456",
                street = "Rua Cami",
            ),
            income = BigDecimal.valueOf(1000.0),
            id = 1L
        ),
        creditCode: UUID = UUID.fromString("aa547c0f-9a6a-451f-8c89-afddce916a29"),
        creditValue: BigDecimal = BigDecimal.valueOf(1000.0),
        dayFirstInstallment: LocalDate = LocalDate.now().plusMonths(2),
        numberOfInstallments: Int = 5,
        status: Status = Status.IN_PROGRESS,
        ) = Credit(
        id = id,
        customer = customer,
        creditCode = creditCode,
        creditValue = creditValue,
        dayFirstInstallment = dayFirstInstallment,
        numberOfInstallments = numberOfInstallments,
        status = status,

    )
}