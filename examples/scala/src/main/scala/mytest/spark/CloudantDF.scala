/*******************************************************************************
* Copyright (c) 2015 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package mytest.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkConf
import org.apache.spark.sql.SQLContext
import org.apache.spark.storage.StorageLevel

object CloudantDF{
  def main(args: Array[String]) {
    val spark = SparkSession
      .builder()
      .appName("Cloudant Spark SQL Example with Dataframe")
      .config("cloudant.host","ACCOUNT.cloudant.com")
      .config("cloudant.username", "USERNAME")
      .config("cloudant.password","PASSWORD")
      .config("createDBOnSave","true") // to create a db on save
      .config("jsonstore.rdd.partitions", "20") // using 20 partitions
      .getOrCreate()
          
    // 1. Loading data from Cloudant db
    val df = spark.read.format("com.cloudant.spark").load("n_flight")
    // Caching df in memory to speed computations
    // and not to retrieve data from cloudant again
    df.cache() 
    df.printSchema()

    // 2. Saving dataframe to Cloudant db
    val df2 = df.filter(df("flightSegmentId") === "AA106")
        .select("flightSegmentId","economyClassBaseCost")
    df2.show()
    df2.write.format("com.cloudant.spark").save("n_flight2")
    
    // 3. Loading data from Cloudant search index
    val df3 = spark.read.format("com.cloudant.spark")
      .option("index", "_design/view/_search/n_flights").load("n_flight")
    val total = df3.filter(df3("flightSegmentId") >"AA9")
      .select("flightSegmentId", "scheduledDepartureTime")
      .orderBy(df3("flightSegmentId")).count()
    println(s"Total $total flights from index")

    // 4. Loading data from view
    val df4 = spark.read.format("com.cloudant.spark")
      .option("view", "_design/view/_view/AA0").load("n_flight")
    df4.printSchema()
    df4.show()
  }
}
