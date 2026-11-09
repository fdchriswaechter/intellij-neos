<?php
namespace Vendor\Package\Controller {

    use Neos\Flow\Mvc\Controller\ActionController;

    abstract class AbstractBaseController extends ActionController
    {
        public function baseAction() {}
    }

    class DirectController extends ActionController
    {
        public function directAction() {}
    }

    class InheritedController extends AbstractBaseController
    {
        public function initializeFooAction() {}
        public function fooAction() {}
        public function helper() {}
        protected function protectedAction() {}
    }

    class NotAController
    {
        public function fooAction() {}
        public function initializeObject() {}
    }
}

namespace Vendor\Package\Command {

    use Neos\Flow\Cli\CommandController;

    abstract class AbstractBaseCommandController extends CommandController {}

    class InheritedCommandController extends AbstractBaseCommandController
    {
        public function runCommand() {}
    }
}
