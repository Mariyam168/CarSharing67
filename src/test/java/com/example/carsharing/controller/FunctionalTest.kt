import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.time.Duration
import java.time.Instant

object FunctionalTest {
    private const val LOG_FILE = "functional_test_log.txt"

    @Synchronized
    private fun logToFile(message: String) {
        try {
            PrintWriter(FileWriter(LOG_FILE, true)).use { out ->
                out.printf("%s - %s%n", Instant.now(), message)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        // Обнуляем лог
        try {
            PrintWriter(FileWriter(LOG_FILE, false)).use { /* очищаем файл */ }
        } catch (ignored: IOException) {}

        logToFile("=== Функциональный тест: старт ===")
        val driver: WebDriver = ChromeDriver()
        try {
            logToFile("Открываем страницу логина")
            driver.get("http://localhost:3000/login")

            driver.findElement(By.name("email"))
                .sendKeys("supersamat2004@gmail.com")
            driver.findElement(By.name("password"))
                .sendKeys("supersamat")
            driver.findElement(By.cssSelector("button[type=submit]"))
                .click()
            logToFile("Клик по кнопке \"Войти\"")

            // Используем Duration вместо integer
            WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("http://localhost:3000/"))
            logToFile("Успешный переход на главную страницу")

            println("Авторизация — УСПЕШНО")
            logToFile("Тест завершён успешно")
        } catch (e: Exception) {
            System.err.println("Ошибка в функциональном тестировании: " + e.message)
            logToFile("Ошибка: " + e.message)
        } finally {
            driver.quit()
            logToFile("=== Функциональный тест: конец ===")
        }
    }
}
