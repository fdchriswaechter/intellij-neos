<?php
namespace Vendor\Package\Controller;

class ExampleController
{
    public function someAction(): void
    {
        $this->emitSomethingHappened('payload');
    }

    protected function emitSomethingHappened(string $payload): void
    {
    }
}
