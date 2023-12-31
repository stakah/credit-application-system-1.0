package me.dio.credit.application.system.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.exception.BusinessException
import me.dio.credit.application.system.repository.CustomerRepository
import me.dio.credit.application.system.service.impl.CustomerService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.util.*

//@ActiveProfiles("test")
@ExtendWith(MockKExtension::class)
class CustomerServiceTest {
    @MockK lateinit var customerRepository: CustomerRepository
    @InjectMockKs lateinit var customerService: CustomerService

    @Test
    fun `should create customer`() {
        //given
        val fakeCustomer: Customer = buildCustomer();
        every { customerRepository.save(any()) } returns fakeCustomer

        //when
        val actualCustomer: Customer = customerService.save(fakeCustomer)

        //expect
        Assertions.assertThat(actualCustomer).isNotNull
        Assertions.assertThat(actualCustomer).isSameAs(fakeCustomer)
        verify (exactly = 1) {customerService.save(fakeCustomer)}
    }

    @Test
    fun `should find customer by id`() {
        //given
        val fakeId: Long = Random().nextLong()
        val fakeCustomer: Customer = buildCustomer(id = fakeId)
        every { customerRepository.findById(fakeId) } returns Optional.of(fakeCustomer)

        //when
        val actualCustomer: Customer = customerService.findById(fakeId)

        //expect
        Assertions.assertThat(actualCustomer).isNotNull
        Assertions.assertThat(actualCustomer).isExactlyInstanceOf(Customer::class.java)
        Assertions.assertThat(actualCustomer).isSameAs(fakeCustomer)
        verify (exactly = 1){ customerRepository.findById(fakeId) }
    }

    @Test
    fun `should not find customer by id and throw BusinessException`() {
        //given
        val fakeId: Long = Random().nextLong()
        every { customerRepository.findById(fakeId) } returns Optional.empty()

        //when

        //expect
        Assertions.assertThatExceptionOfType(BusinessException::class.java)
            .isThrownBy { customerService.findById(fakeId) }
            .withMessage("Id $fakeId not found")
        verify (exactly = 1){ customerRepository.findById(fakeId) }
    }

    @Test
    fun `should delete customer by id`() {
        //given
        var fakeId: Long = Random().nextLong()
        var fakeCustomer: Customer = buildCustomer(id = fakeId)
        every { customerRepository.findById(fakeId) } returns Optional.of(fakeCustomer)
        every { customerRepository.delete(fakeCustomer) } just runs

        //when
        customerService.delete(fakeId)

        //expect
        verify (exactly = 1){ customerRepository.findById(fakeId) }
        verify (exactly = 1){ customerRepository.delete(fakeCustomer)  }

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
}