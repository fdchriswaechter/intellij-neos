<?php
namespace Vendor\Package;

use Neos\Flow\Core\Bootstrap;
use Vendor\Package\Controller\ExampleController;
use Vendor\Package\Service\ExampleService;

class PackageForHighlighting
{
    public function boot(Bootstrap $bootstrap): void
    {
        $dispatcher = $bootstrap->getSignalSlotDispatcher();
        $dispatcher->connect(ExampleController::class, 'somethingHappened', ExampleService::class, 'handleSomethingHappened');
    }
}
