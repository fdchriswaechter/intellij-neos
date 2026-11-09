<?php
namespace Vendor\Package;

use Neos\Flow\Core\Bootstrap;
use Vendor\Package\Controller\ExampleController;
use Vendor\Package\Service\ExampleService;

class PackageWithNamedArguments
{
    public function boot(Bootstrap $bootstrap): void
    {
        $dispatcher = $bootstrap->getSignalSlotDispatcher();
        $dispatcher->connect(ExampleController::class, slotMethodName: 'handleSometh<caret>ingHappened', signalName: 'someth<caret>ingHappened', slotClassNameOrObject: ExampleService::class);
    }
}
