package me.dio.credit.application.system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.dto.request.CustomerUpdateDto
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CustomerRepository
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import java.util.regex.Matcher

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {
    @Autowired private lateinit var customerRepository: CustomerRepository
    @Autowired private lateinit var creditRepository: CreditRepository
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    companion object {
        const val URL: String = "/api/credits"
    }

    @BeforeEach fun setup() {
        customerRepository.deleteAll()
        creditRepository.deleteAll()
    }
    @AfterEach fun tearDown() {
        customerRepository.deleteAll()
        creditRepository.deleteAll()
    }

    @Test
    fun `should create a credit and return 201 status`() {
        //given
        val customer: Customer = customerRepository.save(builderCustomerDto().toEntity())
        val creditDto: CreditDto = builderCreditDto(customer.id!!)
        val valueAsString:String = objectMapper.writeValueAsString(creditDto)

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString))
            .andExpect(MockMvcResultMatchers.status().isCreated)
            .andDo(MockMvcResultHandlers.print())
    }


    @Test
    fun `should not create a credit when dayFirstInstallment is beyond 3 months from now and return 400 status`() {
        //given
        customerRepository.save(builderCustomerDto().toEntity())
        val creditDto: CreditDto = builderCreditDto(dayFirstOfInstallment = LocalDate.now().plusMonths(4))
        val valueAsString:String = objectMapper.writeValueAsString(creditDto)

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(valueAsString)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class me.dio.credit.application.system.exception.BusinessException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.null").value("Invalid Date"))

            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should find credit by creditCode and return 200 status`() {
        //given
        val customerDto : CustomerDto  = builderCustomerDto()
        val customer: Customer = customerRepository.save(customerDto.toEntity())
        val creditDto: CreditDto = builderCreditDto()
        val credit: Credit = creditRepository.save(creditDto.toEntity())

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL/${credit.creditCode}?customerId=${customer.id}").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").value(credit.creditCode.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value(creditDto.creditValue))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value(creditDto.numberOfInstallments))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(credit.status.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value(customerDto.email))
            .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value(customerDto.income))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not find credit by invalid creditCode and return 400 status`() {
        //given
        val invalidCreditCode: UUID = UUID.randomUUID()
        val customerId: Long = 1L;

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL/${invalidCreditCode.toString()}?customerId=${customerId}").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class me.dio.credit.application.system.exception.BusinessException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.null").value("Creditcode ${invalidCreditCode.toString()} not found"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not find credit by missing customerId argument and return 400 status`() {
        //given
        val invalidCreditCode: UUID = UUID.randomUUID()

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL/${invalidCreditCode.toString()}").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.status().reason("Required parameter 'customerId' is not present."))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not find credit by customerId not given and return 400 status`() {
        //given
        val invalidCreditCode: UUID = UUID.randomUUID()

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL/${invalidCreditCode.toString()}?customerId=").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class java.lang.NumberFormatException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.null").value("For input string: \"\""))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not find credit for invalid customer and return 400 status`() {
        //given
        val customerDto : CustomerDto  = builderCustomerDto()
        val customer: Customer = customerRepository.save(customerDto.toEntity())
        val creditDto: CreditDto = builderCreditDto(customerId = customer.id!!)
        val credit: Credit = creditRepository.save(creditDto.toEntity())
        val invalidCustomerId: Long = -customer.id!!;

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL/${credit.creditCode}?customerId=${invalidCustomerId}").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class java.lang.IllegalArgumentException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.null").value("Contact admin"))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should return list of credits for a customer and return 200 status`() {
        //given
        val customerDto : CustomerDto  = builderCustomerDto()
        val customer: Customer = customerRepository.save(customerDto.toEntity())
        val creditDto1: CreditDto = builderCreditDto(
            customerId = customer.id!!,
            numberOfInstallments = 3,
            dayFirstOfInstallment = LocalDate.now().plusMonths(2))
        val creditDto2: CreditDto = builderCreditDto(
            customerId = customer.id!!,
            numberOfInstallments = 4,
            dayFirstOfInstallment = LocalDate.now().plusMonths(3))
        val credit1: Credit = creditRepository.save(creditDto1.toEntity())
        val credit2: Credit = creditRepository.save(creditDto2.toEntity())

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL?customerId=${customer.id}").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[*].creditCode",
                Matchers.contains(
                    credit1.creditCode.toString(),
                    credit2.creditCode.toString(),
                    )
            ))
            .andDo(MockMvcResultHandlers.print())
    }
//    jsonPath("$.error[?(@.errorMessage=='Fixed Error Message')]").exists
    @Test
    fun `should not return list of credits for an unknown customer and return 400 status`() {
        //given
        val unknownCustomerId: Long = 2L;

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL?customerId=${unknownCustomerId}").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$").isArray)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(0)))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not return list of credits for customerId not given and return 400 status`() {
        //given

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL?customerId=").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.exception").value("class java.lang.NumberFormatException"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.details.null").value("For input string: \"\""))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not return list of credits for unrestricted request and return 400 status`() {
        //given

        //when
        //expect
        mockMvc.perform(MockMvcRequestBuilders.get("$URL").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.status().reason("Required parameter 'customerId' is not present."))
            .andDo(MockMvcResultHandlers.print())
    }


    private fun builderCreditDto(
        customerId: Long = 1L,
        creditValue: BigDecimal = BigDecimal.valueOf(1000.0),
        numberOfInstallments: Int = 5,
        dayFirstOfInstallment: LocalDate = LocalDate.now().plusMonths(2)

    ) : CreditDto = CreditDto(
        customerId = customerId,
        creditValue = creditValue,
        numberOfInstallments = numberOfInstallments,
        dayFirstOfInstallment = dayFirstOfInstallment,
    )

    private fun builderCustomerDto(
        firstName: String = "Cami",
        lastName: String = "Cavalcante",
        cpf: String = "28475934625",
        email: String = "cami@gmail.com",
        income: BigDecimal = BigDecimal.valueOf(1000.0),
        password: String = "1234",
        zipCode: String = "000000",
        street: String = "Rua da Cami, 123",
    )  : CustomerDto = CustomerDto(
        firstName = firstName,
        lastName = lastName,
        cpf = cpf,
        income = income,
        email = email,
        password = password,
        zipCode = zipCode,
        street = street,

    )

    private fun bulderCustomerUpdateDto(
        firstName: String = "CamiUpdate",
        lastName: String = "CavalcanteUpdate",
        income: BigDecimal = BigDecimal.valueOf(5000.0),
        zipCode: String = "45656",
        street: String = "Rua Updated"
    ): CustomerUpdateDto = CustomerUpdateDto(
        firstName = firstName,
        lastName = lastName,
        income = income,
        zipCode = zipCode,
        street = street,
    )
}