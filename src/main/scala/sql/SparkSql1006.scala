package sql

import java.io.File


import org.apache.spark.sql.functions
import org.apache.spark.sql.SparkSession
import utils.FileUtils

object SparkSql1006 extends App {
  FileUtils.dirDel(new File("D:/result"))
  val spark = SparkSession
    .builder()
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  spark
    .read
    .option("header", "true")
    .option("delimiter", ",")
    .csv("D:\\login.csv")
    .createOrReplaceTempView("login")

  spark
    .read
    .option("header", "true")
    .option("delimiter", ",")
    .csv("D:\\order_info.csv")
    .createOrReplaceTempView("order_info")

  val avg = 16.720800495990105
  spark
    .sql(
      s"""
         | select
         |   oi.device_id,
         |   case
         |     when sum(oi.shipping_fee + oi.goods_amount) / count(1) <= 1/2 * ${avg} then '0-1/2_price'
         |     when sum(oi.shipping_fee + oi.goods_amount) / count(1) <= ${avg} then '1/2-1_price'
         |     when sum(oi.shipping_fee + oi.goods_amount) / count(1) <= 2 * ${avg} then '1-2_price'
         |     else '2-'
         |    end as avg
         | from order_info oi
         | group by device_id
          """.stripMargin)
    .createOrReplaceTempView("consume")


  spark
    .sql(
      s"""
         | select *
         | from
         | (select
         |   *,
         |  row_number()
         |   over (partition by device_id order by pay_time desc)
         |     as rank
         | from order_info
         | ) as tmp
         | where tmp.rank = 1
    """.stripMargin)
    .createOrReplaceTempView("first")

  spark
    .sql(
      s"""
         | select *
         | from
         | (select
         |   *,
         |  row_number()
         |   over (partition by device_id order by pay_time desc)
         |     as rank
         | from order_info
         | ) as tmp
         | where tmp.rank = 2
    """.stripMargin)
    .createOrReplaceTempView("second")

  spark
    .sql(
      s"""
         | select
         |   case
         |     when datediff(to_date(f.pay_time), to_date(s.pay_time)) <= 15 then '0-15'
         |     when datediff(to_date(f.pay_time), to_date(s.pay_time)) <= 30 then '15-30'
         |     when datediff(to_date(f.pay_time), to_date(s.pay_time)) <= 45 then '30-45'
         |     when datediff(to_date(f.pay_time), to_date(s.pay_time)) <= 60 then '45-60'
         |     else '60-'
         |  end as intervals,
         |  f.device_id
         | from first f
         |   inner join second s using(device_id)
    """.stripMargin)
    .createOrReplaceTempView("activity")


  spark
    .sql(
      """
        | select
        |   oi.device_id,
        |   case
        |    when count(1) = 1 then '1_ordered'
        |    when count(1) = 2 then '2_ordered'
        |    when count(1) = 3 then '3_ordered'
        |    when count(1) = 4 then '4_ordered'
        |    when count(1) = 5 then '5_ordered'
        |   else
        |    'more_than_5_ordered'
        |   end as freq
        | from order_info oi
        | group by device_id
      """.stripMargin)
    .createOrReplaceTempView("frequent")


  spark
    .sql(
      s"""
         | select
         |   *
         | from login
         |   left join consume using (device_id)
         |   left join activity using (device_id)
         |   left join frequent using (device_id)
          """.stripMargin)
    .coalesce(1)
    .write
    .option("header", "true")
    .option("delimiter", ",")
    .csv("d:\\result")


}

object SparkSql1006_4 extends App {


  val spark = SparkSession
    .builder()
    .master("local[*]")
    .getOrCreate()

  spark
    .read
    .option("header", "true")
    .option("delimiter", ",")
    .csv("D:\\device_tags.csv")
    .createOrReplaceTempView("tmp")

  val tmp = spark
    .sql(
      s"""
         | select
         |  tmp.*
         | from
         |  tmp
         |  inner join tmp tmp1 using(device_id)
         | where datediff(to_date(tmp1.action_date), to_date(tmp.action_date)) = 1
      """.stripMargin)

  //视图
  List("avg", "intervals", "freq") foreach (key => {
    FileUtils.dirDel(new File(s"d:/$key"))
    tmp
      .groupBy("action_date")
      .pivot(s"$key")
      .count()
      .coalesce(1)
      .write
      .option("header", "true")
      .option("delimiter", ",")
      .csv(s"d:/{$key}_next_day")
  })


}


object SparkSql1006_5 extends App {

  FileUtils.dirDel(new File("d:/last_click_list_type"))

  val spark = SparkSession
    .builder()
    .master("local[*]")
    .getOrCreate()

  spark
    .read
    .option("header", "true")
    .option("delimiter", ",")
    .csv("D:\\order_cause.csv")
    .createOrReplaceTempView("order_cause")

  spark
    .read
    .option("header", "true")
    .option("delimiter", ",")
    .csv("D:\\order_info.csv")
    .createOrReplaceTempView("order_info")

  val tmp = spark
    .sql(
      s"""
         | select
         |  to_date(oi.order_time) as order_date,
         |  last_click_list_type,
         |  pay_status,
         |  count(1)
         | from order_info oi
         |  inner join order_cause oc on oc.order_goods_rec_id = oi.rec_id
         |  group by order_date, last_click_list_type,pay_status
      """.stripMargin)
    .coalesce(1)
    .write
    .option("header", "true")
    .option("delimiter", ",")
    .csv("d:/last_click_list_type")


}