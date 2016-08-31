#Hibernate Table Filter
    Provides simple selecting rows from hibernate table
    by passing parameters through REST interface.
    It help to create JS SPA without parameters mapping on server's side.

##Example
###GET action
```java
@RequestMapping(value = "/users", method = RequestMethod.GET)
public ResponseEntity operators(@ModelAttribute TableFilter tableFilter) throws Exception
{
    return new ResponseEntity(tableFilter.run(UserEntity.class, session), HttpStatus.OK);
}
```
###POST action (and other methods)
```java
@RequestMapping(value = "/users", method = RequestMethod.POST)
public ResponseEntity operators(@RequestBody TableFilter tableFilter) throws Exception
{
    return new ResponseEntity(tableFilter.run(UserEntity.class, session), HttpStatus.OK);
}
```
###GET request
```
GET: /users?
like[login]=use
&ge[registrationDate]=2010-10-10
&in[role.id][0]=1
&in[role.id][1]=10
&in[role.id][1]=12
&lt[friendsCount]=10
&like[email,secondEmail]=google.com
&nullable[password]=false
&eq[invited.role.id]=10
&order[id]=false
&limit=20
&offset=0
```
###POST request (json)
```
POST: /users
{
    "like":{
        "login":"use",
        "email,secondEmail":"google.com"
    },
    "ge":{
        "registrationDate":"2010-10-10"
    },
    "in":{
        "role.id":[
            1,
            10,
            12
        ]
    },
    "lt":{
        "friendsCount":10
    },
    "nullable":{
        "password":false
    },
    "eq":{
        "invited.role.id":10
    },
    "order":{
        "id":false
    },
    "limit":20,
    "offset":0
}
```

##All properties
```java
private Map<String, String> eq;
private Map<String, String> gt;
private Map<String, String> lt;
private Map<String, String> ge;
private Map<String, String> le;
private Map<String, String> like;
private Map<String, Boolean> order;
private Map<String, List<String>> in;
private Map<String, Boolean> nullable;
private int offset = 0;
private int limit = 20;
```
##Execution method
    table - hibernate entity class
    session - hibernate session
```java
public List run(Class table, Session session) throws Exception
```
##Features
    If you use dot-separated field name (example "invited.id")
    and main class has property (annotated @ManyToOne etc.)
    filter would be search by foreign table.
    Like-expression supports comma-separated values (example "email,secondEmail")
    that make "or like" sql expression by many fields.