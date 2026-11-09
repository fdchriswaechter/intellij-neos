<?php
namespace Vendor\Package\Domain\Model {
    class Company
    {
        public function getName(): string {}
    }

    class Job
    {
    }
}

namespace Vendor\Package\Domain\Repository {

    use Neos\Flow\Persistence\Repository;
    use Vendor\Package\Domain\Model\Job;

    class CompanyRepository extends Repository
    {
        public function findByCustomCriteria(): array {}
    }

    abstract class AbstractElasticRepository extends \Neos\Flow\Persistence\Doctrine\Repository
    {
        const ENTITY_CLASSNAME = Job::class;
    }

    class JobOfferRepository extends AbstractElasticRepository
    {
    }
}

namespace Vendor\Package\Service {
    class NotARepository
    {
        public function __call($method, $arguments) {}
    }
}
