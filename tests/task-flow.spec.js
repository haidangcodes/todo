import { test, expect } from '@playwright/test';

const BASE_URL = 'http://127.0.0.1:8080';

function localDateTimeValue(date = new Date()) {
  const offsetMs = date.getTimezoneOffset() * 60 * 1000;
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

test('register if needed, create task, filter today, complete task', async ({ page }) => {
  const email = `test-${Date.now()}@example.com`;
  const password = '123456';
  const name = 'Playwright User';

  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });

  await page.getByRole('link', { name: 'Sign up', exact: true }).click();
  await page.locator('#name').fill(name);
  await page.locator('#email').fill(email);
  await page.locator('#password').fill(password);
  await page.locator('#emailSubmitBtn').click();

  await expect(page.locator('#fabAdd')).toBeVisible({ timeout: 15000 });

  await page.locator('#fabAdd').click();
  await page.locator('#taskTitle').waitFor({ state: 'visible', timeout: 10000 });

  await page.locator('#taskTitle').fill('Playwright Task');
  await page.locator('#taskDesc').fill('Task created by Playwright');
  await page.locator('#taskCategory').selectOption('work');
  await page.locator('#taskPriority').selectOption('high');
  await page.locator('#taskDeadline').fill(localDateTimeValue());

  await page.locator('#modalSubmit').click();
  await expect(page.getByText('Playwright Task', { exact: true })).toBeVisible({ timeout: 15000 });

  await page.locator('[data-filter="today"]').click();
  await expect(page.getByText('Playwright Task', { exact: true })).toBeVisible({ timeout: 15000 });

  await page.locator('.task .task__checkbox').first().click();
  await expect(page.locator('.task.completed')).toHaveCount(1);
});
