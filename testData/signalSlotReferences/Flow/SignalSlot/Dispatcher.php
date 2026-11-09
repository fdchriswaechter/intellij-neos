<?php
namespace Neos\Flow\SignalSlot;

class Dispatcher
{
    public function connect(
        string $signalClassName,
        string $signalName,
        $slotClassNameOrObject,
        string $slotMethodName = '',
        bool $passSignalInformation = true
    ): void {
    }
}
