

import org.leibnizcenter.lppneu.components.language.Program
import org.leibnizcenter.lppneu.parser.LPPNLoader

class DecoratorTest extends GroovyTestCase {

//    void testDecorationCrywolf() {
//
//        Program program = LPPNLoader.parseFile("examples/basic/crywolf.lppn")
//
//        program.print()
//
//    }

    void testDecorationYaleshooting() {

        Program program = LPPNLoader.parseFile("examples/basic/yaleshooting.lppn")

        program.print()

    }

//    void testDecorationLogicRules() {
//
//        Program program = LPPNLoader.parseFile("examples/basic/test_logicrules.lppn")
//
//        program.print()
//
//    }

}