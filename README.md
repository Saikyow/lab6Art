# Lab6 — Client-Server City Collection Manager

Клиент-серверное приложение для управления коллекцией объектов `City`. Сервер хранит коллекцию в XML-файле, клиент в интерактивном режиме отправляет команды по TCP.

Проект соответствует требованиям ЛР5 (модель, валидация, XML, переменная окружения с путём к файлу) и ЛР6 (разделение на client/server, NIO non-blocking сервер, сериализованный обмен, retry на клиенте, логирование).

## Архитектура

```
+-----------------+        TCP (Object Streams)        +-----------------+
|     Client      |  <------------------------------>  |     Server      |
|                 |                                    |                 |
| ConsoleInput    |                                    | NIO Selector    |
| CityFormReader  |                                    | Acceptor        |
| TcpClient       |                                    | RequestReader   |
| ClientMain      |                                    | Dispatcher      |
+-----------------+                                    | ResponseSender  |
                                                       | XmlRepository   |
                                                       | CollectionMgr   |
                                                       +-----------------+
                                                              |
                                                              v
                                                       +-----------------+
                                                       |  cities.xml     |
                                                       +-----------------+
```

- **common** — общие сериализуемые классы: модель (`City`, `Coordinates`, `Human`, `StandardOfLiving`, `CityData`) и протокол (`Request`, `Response`, `CommandName`).
- **server** — XML-хранилище, менеджер коллекции (`LinkedHashSet` + Stream API), однопоточный неблокирующий TCP-сервер на `Selector`, диспетчер команд.
- **client** — построчный ввод команд, парсер форм с подсказками и повтором при ошибке, TCP-клиент с повторами при недоступности сервера.

## Требования

- JDK 17+
- Maven 3.8+

## Сборка

```bash
cd Lab6
mvn clean package -DskipTests
```

После сборки появятся:
- `server/target/server-1.0.0.jar` и зависимости в `server/target/lib/`
- `client/target/client-1.0.0.jar` и зависимости в `client/target/lib/`

## Запуск

### Сервер (терминал #1)

```bash
cd Lab6
export CITIES_FILE=$PWD/sample/cities.xml
export SERVER_PORT=5555
java -cp "server/target/server-1.0.0.jar:server/target/lib/*" lab6.server.ServerMain
```

В логе должно появиться:
```
INFO  [ServerMain] Загружено элементов: N из ...
INFO  [NioServer] Сервер слушает порт 5555
Серверные команды: save, exit
```

Команды серверной консоли (недоступны клиенту):
- `save` — сохранить коллекцию в XML.
- `exit` — сохранить и завершить работу.
- `Ctrl-C` — корректное завершение (shutdown hook сохраняет коллекцию).

### Клиент (терминал #2)

```bash
cd Lab6
export SERVER_HOST=localhost
export SERVER_PORT=5555
java -cp "client/target/client-1.0.0.jar:client/target/lib/*" lab6.client.ClientMain
```

После старта появится приглашение `>`. Если сервер недоступен — клиент делает 5 попыток с интервалом 2 секунды и продолжает работу (не падает).

## Переменные окружения

| Переменная | Сторона | По умолчанию | Описание |
|---|---|---|---|
| `CITIES_FILE` | server | — (обязательно) | путь к XML-файлу с коллекцией |
| `SERVER_PORT` | server, client | `5444` | TCP-порт |
| `SERVER_HOST` | client | `localhost` | адрес сервера |

## Команды клиента

| Команда | Аргументы | Описание |
|---|---|---|
| `help` | — | справка по командам |
| `info` | — | тип коллекции, дата инициализации, число элементов |
| `show` | — | вывести все элементы (отсортированы по местоположению) |
| `add` | объект `City` (поля по строкам) | добавить новый элемент |
| `update` | `id`, затем объект `City` | обновить элемент по id |
| `remove_by_id` | `id` | удалить элемент по id |
| `clear` | — | очистить коллекцию |
| `execute_script` | `file_name` | выполнить скрипт |
| `exit` | — | завершить работу клиента (без save) |
| `add_if_max` | объект `City` | добавить, если больше максимального |
| `remove_greater` | объект `City` | удалить элементы больше заданного |
| `remove_lower` | объект `City` | удалить элементы меньше заданного |
| `min_by_name` | — | элемент с минимальным `name` |
| `filter_by_timezone` | `timezone` | элементы с указанным `timezone` |
| `filter_less_than_standard_of_living` | `standardOfLiving` | элементы с уровнем жизни ниже заданного |

## Модель City

```java
class City {
    int id;                            // > 0, генерируется сервером
    String name;                       // не пустая
    Coordinates coordinates;           // не null
    ZonedDateTime creationDate;        // генерируется сервером
    Double area;                       // > 0, не null
    Long population;                   // > 0, не null
    int metersAboveSeaLevel;
    double timezone;                   // (-13; 15]
    int carCode;                       // (0; 1000]
    StandardOfLiving standardOfLiving; // может быть null
    Human governor;                    // не null
}

class Coordinates {
    long x;     // > -690
    Float y;    // не null
}

class Human {
    String name;  // не пустая
}

enum StandardOfLiving { NIGHTMARE, ULTRA_LOW, LOW }
```

## Скрипты `execute_script`

Каждая строка — команда того же вида, что и в интерактивном режиме. Поля составных объектов (`City`) идут сразу за командой, по одному на строку. Пустая строка означает `null` (для `standardOfLiving`).

Пример (`sample/smoke.txt` в репозитории):
```
info
show
min_by_name
filter_by_timezone 3.0
filter_less_than_standard_of_living LOW
remove_by_id 2
show
exit
```

Порядок полей при `add` / `update`:
1. `name`
2. `coordinates.x`
3. `coordinates.y`
4. `area`
5. `population`
6. `metersAboveSeaLevel`
7. `timezone`
8. `carCode`
9. `standardOfLiving` (пустая строка = `null`)
10. `governor.name`

При вложенных `execute_script` детектируется рекурсия и зацикленный скрипт пропускается.

## Формат XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<cities>
    <city>
        <id>1</id>
        <name>Saint Petersburg</name>
        <coordinates>
            <x>100</x>
            <y>50.5</y>
        </coordinates>
        <creationDate>2025-01-01T12:00:00+03:00[Europe/Moscow]</creationDate>
        <area>1439.0</area>
        <population>5000000</population>
        <metersAboveSeaLevel>10</metersAboveSeaLevel>
        <timezone>3.0</timezone>
        <carCode>78</carCode>
        <standardOfLiving>LOW</standardOfLiving>
        <governor>
            <name>Ivan</name>
        </governor>
    </city>
</cities>
```

Чтение — через `java.io.FileReader` (DOM-парсер `javax.xml.parsers`). Запись — через `java.io.FileOutputStream`.

## Структура проекта

```
Lab6/
├── pom.xml                       # parent POM
├── README.md
├── sample/
│   ├── cities.xml                # пример коллекции
│   └── smoke.txt                 # пример скрипта
├── common/
│   └── src/main/java/lab6/common/
│       ├── model/                # City, Coordinates, Human, StandardOfLiving, CityData
│       └── net/                  # Request, Response, CommandName
├── server/
│   ├── src/main/java/lab6/server/
│   │   ├── ServerMain.java
│   │   ├── core/                 # CollectionManager
│   │   ├── io/                   # XmlCityRepository
│   │   ├── commands/             # ServerCommand, CommandDispatcher
│   │   └── net/                  # NioServer + 4 модуля
│   └── src/main/resources/
│       └── log4j2.xml
└── client/
    └── src/main/java/lab6/client/
        ├── ClientMain.java
        ├── input/                # CityFormReader, ClientCommand
        ├── io/                   # ConsoleInput, ScriptInput, InputSource
        └── net/                  # TcpClient
```

## Логирование

Серверные события (старт, новые подключения, входящие запросы, отправка ответов, ошибки, остановка) пишутся в `server.log` и stdout через Log4j2. Конфигурация в `server/src/main/resources/log4j2.xml`.

## Быстрая проверка

```bash
# терминал #1
cd Lab6
export CITIES_FILE=$PWD/sample/cities.xml
export SERVER_PORT=5555
java -cp "server/target/server-1.0.0.jar:server/target/lib/*" lab6.server.ServerMain

# терминал #2
cd Lab6
export SERVER_PORT=5555
java -cp "client/target/client-1.0.0.jar:client/target/lib/*" lab6.client.ClientMain < sample/smoke.txt
```

Ожидаемый вывод клиента — последовательность ответов сервера на каждую команду из скрипта.

## .gitignore

Перед публикацией убедись, что в репозиторий не попадают артефакты сборки:

```
target/
*.class
*.jar
*.log
.idea/
*.iml
```
