### presentation announce
#### usage
1. parse raw data form session
2. make application able to observable interest data

#### how to custom a command
simulate count operation with mongo CRUD json server and it's client
1. define structure Query json
```
{
  "taskId": "thread-1:timestamp",//if not contains, means needn't call back
  "dataBase": "helloworld",
  "collection": "accounts",
  "method": "count",
  "params": {
    {"$or": [
      {"username": "administrator"},
      {"phone":"110"},
      {"email":"123@xxx.com"},
      {"pen_name":"lorancechen"}
    }]},
    {"password": "12345_md5"}
  }
}
```
Response json
```
{
  "taskId": "thread-1:timestamp",
  "count": 1
}
```
2. define sender and accept class at client side
 ```
 trait Account
 case class Username(userName: String) extends Account
 case class Phone(phone: String) extends Account
 case class Email(email: String) extends Account
 case class PenName(pen_name: String) extends Account
 
 trait Mongo
 case class DataBase(dataBase: String)
 case class Collection(collection: String)
 case class Method(method: String)
 case class LoginVerify(taskId: String, `$or`: List[Account])
 case class LoginVerifyResult(taskId: String, )
 ```
3. define a observable to dispatch the count result event or exception(eg. timeout)

#### with json
 json has pretty structure to express data and it's easy and lightweight begin with json.
 
 1. parse json:
 with lift-json extractor, we able to use case class to define structure and parse it,
 it's good to detect error if data structure NOT match.
 2. ... todo
 