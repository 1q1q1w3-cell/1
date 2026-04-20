# English Cards (Android MVP)

Нативное офлайн Android-приложение на Kotlin + Jetpack Compose для изучения английских карточек.

## Реализовано

- Загрузка карточек из `app/src/main/assets/data.txt`
- Парсинг формата `front<back>`
- Сохранение статистики в `filesDir/stats.json`
- Логика весов, включая double-confirm hide (`<= -0.8` + swipe know => `-1.0`)
- Свайпы: влево = не знаю, вправо = знаю
- Кнопка `Показать перевод`
- Цвет front-текста по весу
- Взвешенный выбор следующей карточки (`chance_power`, `min_chance`)
- Поиск и воспроизведение `audio/*.wav` (normal/slow)
- Inline spoiler для `image/*.png` с crop верхнего левого `512x512`
- Запись проблемных карточек в `filesDir/output.txt` без дублей
- Экран настроек (MVP), DataStore-репозиторий для параметров
- Фоновый режим через `ReminderWorker` (уведомления)

## Стек

- Kotlin
- Jetpack Compose (Material3)
- DataStore Preferences
- WorkManager
- kotlinx.serialization

