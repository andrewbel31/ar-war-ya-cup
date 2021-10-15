# Приложение Войнушка для Yandex Cup

### Как работает приложение?

#### API 21+, MVI + RxJava2 + Custom views + TensorFlow

При заходе в приложение игрок может либо создать новую сессию либо присоединиться к существующей по
ID. Приложение слушает геолокацию пользователя и датчики чтобы определить положение устройства в
пространстве. Каждый раз когда геолокация меняется приложение пушит апдейт своих данных (локации) в
текущей сессии в онлайн базу данных. Другие игроки в этой же сессии получают апдейт, таким образом у
всех актуальные данные.

Так же если в кадр камеры попадает человек приложение постарается его распознать с помощью
TensorFlow. Если человек распознан и приложение видит что в направлении девайса кто-то есть оно
подпишет распознанного человека его именем.

Если при этих условиях нажать на кнопку "огонь" приложение проанализирует насколько вероятно что "
выстрел" попадет в цель (на основании геопозиций игроков и положения девайса в пространстве и
epsilon), и если выстрел попадает - в онлайн базу данных пушится апдейт в том "кого убили" и у всех
игроков обновляется сессия.

Часть с TensorFlow взял из сэмпла TensorFlow по распознаванию объектов. Часть с анализом положения
устройства в пространстве и определением куда "смотрит" девайс взята из open source приложение
OsmAnd.