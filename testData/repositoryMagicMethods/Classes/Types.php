<?php
namespace Vendor\Package\Service;

use Vendor\Package\Domain\Repository\CompanyRepository;
use Vendor\Package\Domain\Repository\JobOfferRepository;

class Types
{
    protected CompanyRepository $companyRepository;

    protected JobOfferRepository $jobOfferRepository;

    protected NotARepository $notARepository;

    public function run()
    {
        $this->companyRepository->findByMemberId(1)->getFirst();
        $this->companyRepository->findOneByName('x')->getName();
        $this->companyRepository->countByActive(true);
        $this->companyRepository->findByCustomCriteria();
        $this->jobOfferRepository->findOneByTitle('x');
        $this->companyRepository->something();
        $this->companyRepository->findBy();
        $this->notARepository->findByFoo();
    }
}
