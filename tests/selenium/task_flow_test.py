from datetime import datetime, timedelta

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.select import Select
from selenium.webdriver.support.ui import WebDriverWait

BASE_URL = "http://127.0.0.1:8080"


def local_datetime_value():
    now = datetime.now()
    return now.strftime("%Y-%m-%dT%H:%M")


def set_datetime_local(driver, element, value):
    driver.execute_script(
        """
        arguments[0].value = arguments[1];
        arguments[0].dispatchEvent(new Event('input', { bubbles: true }));
        arguments[0].dispatchEvent(new Event('change', { bubbles: true }));
        """,
        element,
        value,
    )


options = webdriver.ChromeOptions()
options.add_argument("--start-maximized")

driver = webdriver.Chrome(options=options)
wait = WebDriverWait(driver, 20)

try:
    email = f"test-{int(datetime.now().timestamp() * 1000)}@example.com"
    password = "123456"
    name = "Selenium User"

    driver.get(BASE_URL)

    wait.until(
        EC.element_to_be_clickable((By.XPATH, "//a[@href='#' and normalize-space()='Sign up']"))
    ).click()

    wait.until(EC.visibility_of_element_located((By.ID, "name"))).send_keys(name)
    driver.find_element(By.ID, "email").send_keys(email)
    driver.find_element(By.ID, "password").send_keys(password)
    driver.find_element(By.ID, "emailSubmitBtn").click()

    wait.until(EC.visibility_of_element_located((By.ID, "fabAdd")))

    driver.find_element(By.ID, "fabAdd").click()
    wait.until(EC.visibility_of_element_located((By.ID, "taskTitle")))

    driver.find_element(By.ID, "taskTitle").send_keys("Selenium Task")
    driver.find_element(By.ID, "taskDesc").send_keys("Task created by Selenium")
    Select(driver.find_element(By.ID, "taskCategory")).select_by_value("work")
    Select(driver.find_element(By.ID, "taskPriority")).select_by_value("high")
    set_datetime_local(driver, driver.find_element(By.ID, "taskDeadline"), local_datetime_value())

    driver.find_element(By.ID, "modalSubmit").click()
    wait.until(EC.invisibility_of_element_located((By.ID, "modalOverlay")))
    wait.until(lambda d: len(d.find_elements(By.CSS_SELECTOR, ".task")) > 0)

    task_cards = driver.find_elements(By.CSS_SELECTOR, ".task")
    task_texts = [card.text for card in task_cards]
    assert any("Selenium Task" in text for text in task_texts), task_texts

    wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, '[data-filter="today"]'))).click()
    wait.until(lambda d: len(d.find_elements(By.CSS_SELECTOR, ".task")) > 0)

    task_cards = driver.find_elements(By.CSS_SELECTOR, ".task")
    task_texts = [card.text for card in task_cards]
    assert any("Selenium Task" in text for text in task_texts), task_texts

    wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, ".task .task__checkbox"))).click()
    wait.until(lambda d: any("completed" in card.get_attribute("class") for card in d.find_elements(By.CSS_SELECTOR, ".task")))

    print("Selenium test passed")

finally:
    driver.quit()
