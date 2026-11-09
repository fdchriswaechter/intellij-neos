<?php
namespace Vendor\Package\Service;

use Vendor\Package\Domain\Repository\CompanyRepository;
use Vendor\Package\Domain\Repository\JobOfferRepository;

class Usage
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
        $this->companyRepository-><weak_warning descr="Method 'something' not found in \Vendor\Package\Domain\Repository\CompanyRepository">something</weak_warning>();
        $this->companyRepository-><weak_warning descr="Method 'findBy' not found in \Vendor\Package\Domain\Repository\CompanyRepository">findBy</weak_warning>();
        $this->notARepository-><weak_warning descr="Method 'findByFoo' not found in NotARepository">findByFoo</weak_warning>();
    }
}
