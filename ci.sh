#!/usr/bin/env bash
# Локальный симулятор CI-пайплайна (t9/Б18): те же команды, что и в Jenkinsfile,
# — чтобы проверить «сборку» без живого Jenkins. Запуск: ./ci.sh
set -euo pipefail

echo "==> [stage] Сборка и тесты (unit + H2-слайсы; docker-теги не входят)"
./gradlew test

echo "==> [stage] Allure-отчёт"
./gradlew allureReport

echo "==> [stage] Артефакты"
echo "HTML-отчёт: build/reports/allure-report/index.html"
echo "Результаты тестов: build/reports/tests/test/index.html"
echo "JSON Allure: build/allure-results/ (архивируется в CI для трендов/пересборки отчёта)"

echo "==> ci.sh: OK"
