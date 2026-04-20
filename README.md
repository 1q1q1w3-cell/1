# English Cards (Android MVP)

Нативное офлайн Android-приложение на Kotlin + Jetpack Compose для изучения английских карточек.

## Реализовано

- Загрузка карточек из `app/src/main/assets/data.txt`
- Парсинг формата `front<back>`
- Сохранение статистики в `filesDir/stats.json`
- Логика весов, включая double-confirm hide (`<= -0.8` + swipe know => `-1.0`)
- Оценка карточки свайпами: влево `Не знаю`, вправо `Знаю`
- Кнопка `Показать перевод`
- Цвет front-текста по весу
- Взвешенный выбор следующей карточки (`chance_power`, `min_chance`)
- Поиск и воспроизведение `audio/*.wav` (normal/slow)
- Inline spoiler для `image/*.png` с crop верхнего левого `512x512`
- Экран настроек (DataStore), русские подписи параметров
- Опциональное отображение веса карточки в углу
- Без системы фоновых уведомлений (по запросу — только ручной режим в приложении)

## Стек

- Kotlin
- Jetpack Compose (Material3)
- DataStore Preferences
- kotlinx.serialization


## Совместимость

- Минимальная версия Android: **7.0 (API 24)**
