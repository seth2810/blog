import ExecutionContext.Implicits.global

val lastInteger = new AtomicInteger
def futureInt() = future {
  Thread sleep 2000
  lastInteger incrementAndGet
}

// use callbacks for completion of futures
val a1 = futureInt
val a2 = futureInt
a1.onSuccess {
    case i1 => {
      a2.onSuccess {
        case i2 => println("Sum of values is " + (i1 + i2))
      }
    }
}
Thread sleep 3000

// use for construct to extract values when futures complete
val b1 = futureInt
val b2 = futureInt
for (i1 <- b1; i2 <- b2) yield println("Sum of values is " + (i1 + i2))
Thread sleep 3000

// wait directly for completion of futures
val c1 = futureInt
val c2 = futureInt
println("Sum of values is " + (Await.result(c1, Duration.Inf) +
  Await.result(c2, Duration.Inf)))