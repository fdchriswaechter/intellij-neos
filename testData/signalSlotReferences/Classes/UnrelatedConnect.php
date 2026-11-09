<?php
namespace Vendor\Package;

use Vendor\Package\Controller\ExampleController;
use Vendor\Package\Service\ExampleService;

class EventEmitter
{
    public function connect(string $class, string $event, string $listener, string $method): void
    {
    }
}

class UnrelatedConnect
{
    public function register(EventEmitter $emitter): void
    {
        $emitter->connect(ExampleController::class, 'somethingHappened', ExampleService::class, 'handleSometh<caret>ingHappened');
    }
}
