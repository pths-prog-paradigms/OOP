# OOP
Это задание на ООП и наследование.

## Задание
Напишите классы `Const`, `Add`, `Subtract`, `Multiply`, `Divide`, `Negate`, `Exp`, а также объект `X` 
отвечающие за математические выражения — функции от одной переменной `x`. 
Все они должны наследоваться прямо или опосредованно от класса или интерфейса `Expr`.
У них должны быть метод `toString()`, возвращающий _полноскобочное_ представление 
и оператор `invoke(x: Double): Double`, собственно вычисляющий выражение.
Напишите также метод `toMinimizedString(): String` — если вы не будете делать advanced-вариант, то пусть он просто не делает ничего содержательного (например, возвращает пустую строку)

Пример:
```kotlin
Add(
    Multiply(
        X, Const(2.0)
    ),
    Exp(Negate(X))
)
```
соответствует выражению `x * 2.0 + exp(-x)`. Соответственно, `toString()` должен вернуть `((x) * (2.0)) + (exp(-(x)))`,
а `invoke(1.0)` — `2.3678794411714423`. Для вычисления экспоненты пользуйтесь функцией `kotlin.math.exp`.

Тесты описаны в файле `Tests.kt`, оттуда их можно запустить. 
Всё так же, для тестирования используется специально плохо написанное решение.
Чтобы пропустить выполнение стресс-тестов, поменяйте значение ENABLE_HEAVY_TESTS на false
Решение о том, разносить ли программу по разным файлам, принимать вам (будет принято любое разумное разделение или отсутствие такового при наличии порядка в файле).

При решении задания обратите внимание на принципы наследования: выделение общего кода и общей функциональности. 
Подумайте о том, что можно вынести промежуточные общие классы, обобщающие некоторые из требуемых.

К курсу подъехало полуавтоматическое тестирование, поэтому пушить результаты тестов теперь не нужно.

### Advanced версия

Реализуйте для всех классов также метод `toMinimizedString(): String`, возвращающий представление без лишних скобок.
Например, 
```kotlin
Add(
    Add(
        X, Const(2.0)
    ),
    Negate(
        Divide(
            Negate(X), X
        )
    )
).toMinimizedString()
```
должно вернуть `x + 2.0 + -(-x / x)`. 
Обратите внимание, что в `Divide(Negate(X), X)` унарный минус не требует скобок (`(-x) / x = -(x / x)`), 
а вот, скажем, `Negate(Divide(X, X))` они нужны (вернёт `-(x / x)`). 
Это связано с тем, что в противном случае при разборе более сложных выражений придётся задействовать более глубокий анализ:
`a / -b` не требует скобок в знаменателе, а `a / -(b / c)` требует — хотя по составляющем этого не скажешь.

Полный список приоритетов:
```
x + 2.0               ->  x + 2.0
x - 2.0               ->  x - 2.0
x * 2.0               ->  x * 2.0
x / 2.0               ->  x / 2.0
2.0 + x               ->  2.0 + x
2.0 - x               ->  2.0 - x
2.0 * x               ->  2.0 * x
2.0 / x               ->  2.0 / x
(x + x) + (x + x)     ->  x + x + x + x
(x + x) + (x - x)     ->  x + x + x - x
(x + x) + (x * x)     ->  x + x + x * x
(x + x) + (x / x)     ->  x + x + x / x
(-x) + (-x)           ->  -x + -x
(exp(x)) + (exp(x))   ->  exp(x) + exp(x)
(x + x) - (x + x)     ->  x + x - (x + x)
(x - x) - (x - x)     ->  x - x - (x - x)
(x * x) - (x * x)     ->  x * x - x * x
(x / x) - (x / x)     ->  x / x - x / x
(-x) - (-x)           ->  -x - -x
(exp(x)) - (exp(x))   ->  exp(x) - exp(x)
(x + x) * (x + x)     ->  (x + x) * (x + x)
(x - x) * (x - x)     ->  (x - x) * (x - x)
(x * x) * (x * x)     ->  x * x * x * x
(x / x) * (x / x)     ->  x / x * x / x
(-x) * (-x)           ->  -x * -x
(exp(x)) * (exp(x))   ->  exp(x) * exp(x)
(x + x) / (x + x)     ->  (x + x) / (x + x)
(x - x) / (x - x)     ->  (x - x) / (x - x)
(x * x) / (x * x)     ->  x * x / (x * x)
(x / x) / (x / x)     ->  x / x / (x / x)
(-x) / (-x)           ->  -x / -x
(exp(x)) / (exp(x))   ->  exp(x) / exp(x)
-(x + x)              ->  -(x + x)
-(x - x)              ->  -(x - x)
-(x * x)              ->  -(x * x)
-(x / x)              ->  -(x / x)
-(-x)                 ->  -(-x)
-(exp(x))             ->  -exp(x)
-(2.0)                 ->  -2.0
exp(2.0)              ->  exp(2.0)
exp(-(2.0))            ->  exp(-2.0)
```
