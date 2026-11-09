<?php
namespace Vendor\Package;

use Neos\Flow\Core\Bootstrap;
use Vendor\Package\Service\ExampleService;

class PackageWithSignalMethodName
{
    public function boot(Bootstrap $bootstrap): void
    {
        $bootstrap->getSignalSlotDispatcher()->connect(
            'Vendor\Package\Controller\ExampleController',
            'emitSometh<caret>ingHappened',
            new ExampleService(),
            'handleSometh<caret>ingHappened'
        );
    }
}
