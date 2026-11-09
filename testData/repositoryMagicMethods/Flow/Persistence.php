<?php
namespace Neos\Flow\Persistence {
    interface QueryResultInterface
    {
        public function getFirst();
    }

    interface RepositoryInterface
    {
        public function findAll(): QueryResultInterface;

        public function __call($method, $arguments);
    }

    abstract class Repository implements RepositoryInterface
    {
        public function findAll(): QueryResultInterface {}

        public function __call($method, $arguments) {}
    }
}

namespace Neos\Flow\Persistence\Doctrine {
    abstract class Repository implements \Neos\Flow\Persistence\RepositoryInterface
    {
        public function findAll(): \Neos\Flow\Persistence\QueryResultInterface {}

        public function __call($method, $arguments) {}
    }
}
