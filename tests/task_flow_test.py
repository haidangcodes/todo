from datetime import datetime
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait, Select
from selenium.webdriver.support import expected_conditions as EC

BASE_URL = "http://127.0.0.1:8080"

def local_datetime_value():
    now = datetime.now()
    return now.strftime("%Y-%m-%dT%H:%M")

options = webdriver.ChromeOptions()
options.add_argument("--start-maximized")

driver = webdriver.Chrome(options=options)
wait = WebDriverWait(driver, 15)

try:
    email = f"test-{int(datetime.now().timestamp() * 1000)}@example.com"
    password = "123456"
    name = "Selenium User"

    driver.get(BASE_URL)

    wait.until(EC.element_to_be_clickable((By.XPATH, "//a[@href='#' and normalize-space()='Sign up']"))).click()

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
    driver.find_element(By.ID, "taskDeadline").send_keys(local_datetime_value())

    driver.find_element(By.ID, "modalSubmit").click()

    wait.until(
        EC.visibility_of_element_located(
            (By.XPATH, "//*[contains(@class,'task__title') and normalize-space()='Selenium Task']")
        )
    )

    wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, '[data-filter="today"]'))).click()

    wait.until(
        EC.visibility_of_element_located(
            (By.XPATH, "//*[contains(@class,'task__title') and normalize-space()='Selenium Task']")
        )
    )

    wait.until(EC.element_to_be_clickable((By.CSS_SELECTOR, ".task .task__checkbox"))).click()
    wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, ".task.completed")))

    print("Selenium test passed")

finally:
    driver.quit()