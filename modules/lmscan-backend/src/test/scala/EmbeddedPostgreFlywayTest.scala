import com.opentable.db.postgres.junit.EmbeddedPostgresRules
import com.opentable.db.postgres.embedded.FlywayPreparer
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.junit.runner.Description

class EmbeddedPostgreFlywayTest extends munit.FunSuite {
    def withRule[T <: TestRule](rule: T)(testCode: T => Any): Unit = {
        rule(
            new Statement() {
                override def evaluate(): Unit = testCode(rule)
            },
            Description.createSuiteDescription("JUnit rule wrapper")
        ).evaluate()
    }

    test("test") {
        withRule(EmbeddedPostgresRules.preparedDatabase(FlywayPreparer.forClasspathLocation("db/test", "db/common", "db/seed"))) { 
            preparedDbRule =>
                val c = preparedDbRule.getTestDatabase().getConnection()
                val s = c.createStatement()
                println("////////////////////////////////////////////////////// test of printing created table //////////////////////////////////////////////////////")
                val rs_t = s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")
                while (rs_t.next) {
                    println(rs_t.getString("table_name"))
                }   
                println("////////////////////////////////////////////////////// test of printing seed data //////////////////////////////////////////////////////")
                val rs_s = s.executeQuery("SELECT * FROM public.account")
                while (rs_s.next) {
                    println(
                        "id = %s, address = %s, balance = %s, amount = %s, type = %s, created_at = %s"
                        .format(rs_s.getString("id"), rs_s.getString("address"), rs_s.getString("balance"), rs_s.getString("amount"), rs_s.getString("type"), rs_s.getString("created_at"))
                    )
                }   
        }
    }
}
