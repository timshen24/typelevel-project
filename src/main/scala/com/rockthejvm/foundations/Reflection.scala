package com.rockthejvm.foundations

object Reflection extends App {
  // pain point #1: differentiat types at runtime
  val numbers = List(1, 2, 3)
  numbers match
    case listOfStrings: List[String] => println("list of strings")
    case listOfNumbers: List[Int]    => println("list of numbers")

  // pain point #2: limitation on overloads
  def processList(list: List[Int]): Int = 42
  // def processList(list: List[String]): Int = 45 Compile error!!

  // TypeTags to the rescue
  import scala.reflect.ClassTag
  case class Person(name: String, age: Int)
  // val ttag = ClassTag[Person]
  // println(ttag.)
}
