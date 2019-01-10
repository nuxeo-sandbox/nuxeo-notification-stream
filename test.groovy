def DB_MONGO = "mongodb"
def DB_h2 = "h2"
def DB_PGSQL = "postgresql"
def DB_DEFAULT = "default"
def DB_ALL = "all"

List DB = [DB_h2, DB_MONGO, DB_PGSQL]

def testValues = "testd"

List dbs = [DB_DEFAULT]
String testdbs = "fix/${testValues}/NXP-123123-something-better".split("/").find({ it.startsWith("testdb") })
if (testdbs) {
  dbs = testdbs.substring("testdb-".length()).split("-")
}

if (dbs.contains(DB_ALL)) {
  dbs = DB
}

println dbs
if (dbs.contains(DB_DEFAULT) || dbs.contains(DB_h2)) {
  println "Run - H2"
}
if (dbs.contains(DB_MONGO)) {
  println "Run - MongoDb"
}
if (dbs.contains(DB_PGSQL)) {
  println "Run - PostgreSQL"
}